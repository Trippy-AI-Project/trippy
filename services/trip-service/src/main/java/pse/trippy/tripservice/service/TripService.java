package pse.trippy.tripservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pse.trippy.tripservice.config.RabbitMQConfig;
import pse.trippy.tripservice.dto.request.CreateTripRequest;
import pse.trippy.tripservice.dto.request.UpdateTripRequest;
import pse.trippy.tripservice.dto.response.ParticipantResponse;
import pse.trippy.tripservice.dto.response.TripDetailResponse;
import pse.trippy.tripservice.dto.response.TripPageResponse;
import pse.trippy.tripservice.dto.response.TripResponse;
import pse.trippy.tripservice.exception.ForbiddenException;
import pse.trippy.tripservice.exception.InvalidTripDataException;
import pse.trippy.tripservice.exception.TripNotFoundException;
import pse.trippy.tripservice.model.entity.Participant;
import pse.trippy.tripservice.model.entity.Trip;
import pse.trippy.tripservice.model.enums.ParticipantRole;
import pse.trippy.tripservice.model.enums.ParticipantStatus;
import pse.trippy.tripservice.model.enums.TripStatus;
import pse.trippy.tripservice.model.enums.TripVisibility;
import pse.trippy.tripservice.repository.ParticipantRepository;
import pse.trippy.tripservice.repository.TripRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final ParticipantRepository participantRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public TripResponse createTrip(CreateTripRequest request, UUID userId) {
        validateDates(request.startDate(), request.endDate());

        Trip trip = Trip.builder()
                .title(request.title())
                .destination(request.destination())
                .description(request.description())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .visibility(parseVisibility(request.visibility()))
                .maxParticipants(request.maxParticipants() != null ? request.maxParticipants() : 10)
                .createdBy(userId)
                .build();

        trip = tripRepository.save(trip);

        // Auto-add creator as OWNER participant with ACCEPTED status
        Participant owner = Participant.builder()
                .trip(trip)
                .userId(userId)
                .role(ParticipantRole.OWNER)
                .status(ParticipantStatus.ACCEPTED)
                .joinedAt(Instant.now())
                .build();
        participantRepository.save(owner);

        // Notify user-service so it can upgrade the creator's role to HOST
        publishTripCreatedEvent(trip.getId(), userId);

        return toTripResponse(trip);
    }

    @Transactional(readOnly = true)
    public TripPageResponse listMyTrips(UUID userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "startDate"));
        Page<Trip> tripPage = tripRepository.findTripsByParticipantUserId(userId, pageRequest);

        List<TripResponse> trips = tripPage.getContent().stream()
                .map(this::toTripResponse)
                .toList();

        return new TripPageResponse(
                trips,
                tripPage.getNumber(),
                tripPage.getSize(),
                tripPage.getTotalElements(),
                tripPage.getTotalPages(),
                tripPage.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public TripDetailResponse getTripDetail(UUID tripId, UUID userId) {
        Trip trip = findTripOrThrow(tripId);
        ensureParticipant(tripId, userId);

        List<ParticipantResponse> participants = participantRepository.findByTripId(tripId).stream()
                .map(this::toParticipantResponse)
                .toList();

        return toTripDetailResponse(trip, participants);
    }

    @Transactional
    public TripResponse updateTrip(UUID tripId, UpdateTripRequest request, UUID userId) {
        Trip trip = findTripOrThrow(tripId);
        ensureOwner(tripId, userId);

        if (request.title() != null) {
            trip.setTitle(request.title());
        }
        if (request.destination() != null) {
            trip.setDestination(request.destination());
        }
        if (request.description() != null) {
            trip.setDescription(request.description());
        }
        if (request.startDate() != null) {
            trip.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            trip.setEndDate(request.endDate());
        }
        if (request.startDate() != null || request.endDate() != null) {
            validateDates(trip.getStartDate(), trip.getEndDate());
        }
        if (request.status() != null) {
            trip.setStatus(parseStatus(request.status()));
        }
        if (request.visibility() != null) {
            trip.setVisibility(parseVisibility(request.visibility()));
        }
        if (request.maxParticipants() != null) {
            trip.setMaxParticipants(request.maxParticipants());
        }
        if (request.coverImageUrl() != null) {
            trip.setCoverImageUrl(request.coverImageUrl());
        }

        trip = tripRepository.save(trip);
        return toTripResponse(trip);
    }

    @Transactional
    public void deleteTrip(UUID tripId, UUID userId) {
        Trip trip = findTripOrThrow(tripId);
        ensureOwner(tripId, userId);
        trip.setStatus(TripStatus.CANCELLED);
        tripRepository.save(trip);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private Trip findTripOrThrow(UUID tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
    }

    private void ensureParticipant(UUID tripId, UUID userId) {
        participantRepository.findByTripIdAndUserId(tripId, userId)
                .filter(p -> p.getStatus() == ParticipantStatus.ACCEPTED)
                .orElseThrow(() -> new ForbiddenException(
                        "You are not a participant of this trip"));
    }

    private void ensureOwner(UUID tripId, UUID userId) {
        participantRepository.findByTripIdAndUserId(tripId, userId)
                .filter(p -> p.getRole() == ParticipantRole.OWNER)
                .orElseThrow(() -> new ForbiddenException(
                        "Only the trip owner can perform this action"));
    }

    private void validateDates(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidTripDataException("End date must be on or after start date");
        }
    }

    private TripVisibility parseVisibility(String visibility) {
        if (visibility == null) {
            return TripVisibility.PRIVATE;
        }
        try {
            return TripVisibility.valueOf(visibility.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidTripDataException("Invalid visibility: " + visibility);
        }
    }

    private TripStatus parseStatus(String status) {
        try {
            return TripStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidTripDataException("Invalid status: " + status);
        }
    }

    /**
     * Publishes a {@code trip.created} event to RabbitMQ so that user-service
     * can upgrade the creator's platform role to HOST.
     */
    private void publishTripCreatedEvent(UUID tripId, UUID createdBy) {
        java.util.Map<String, Object> event = java.util.Map.of(
                "eventType", "trip.created",
                "tripId", tripId.toString(),
                "createdBy", createdBy.toString(),
                "timestamp", java.time.Instant.now().toString()
        );
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.TRIP_EXCHANGE, "trip.created", event);
            log.info("Published trip.created event: tripId={}, createdBy={}", tripId, createdBy);
        } catch (AmqpException ex) {
            log.error("Failed to publish trip.created event for tripId={}", tripId, ex);
        }
    }

    private TripResponse toTripResponse(Trip trip) {
        return new TripResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getDestination(),
                trip.getDescription(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getStatus().name(),
                trip.getVisibility().name(),
                trip.getMaxParticipants(),
                trip.getCoverImageUrl(),
                trip.getCreatedBy(),
                trip.getCreatedAt(),
                trip.getUpdatedAt()
        );
    }

    private TripDetailResponse toTripDetailResponse(Trip trip, List<ParticipantResponse> participants) {
        return new TripDetailResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getDestination(),
                trip.getDescription(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getStatus().name(),
                trip.getVisibility().name(),
                trip.getMaxParticipants(),
                trip.getCoverImageUrl(),
                trip.getCreatedBy(),
                trip.getCreatedAt(),
                trip.getUpdatedAt(),
                participants
        );
    }

    private ParticipantResponse toParticipantResponse(Participant p) {
        return new ParticipantResponse(
                p.getId(),
                p.getUserId(),
                p.getRole().name(),
                p.getStatus().name(),
                p.getJoinedAt()
        );
    }
}
