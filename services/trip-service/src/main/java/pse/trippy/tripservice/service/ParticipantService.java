package pse.trippy.tripservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pse.trippy.tripservice.config.RabbitMQConfig;
import pse.trippy.tripservice.dto.event.ParticipantEvent;
import pse.trippy.tripservice.dto.request.InviteParticipantRequest;
import pse.trippy.tripservice.dto.response.ParticipantActionResponse;
import pse.trippy.tripservice.dto.response.ParticipantResponse;
import pse.trippy.tripservice.exception.ForbiddenException;
import pse.trippy.tripservice.exception.InvalidTripDataException;
import pse.trippy.tripservice.exception.TripNotFoundException;
import pse.trippy.tripservice.model.entity.Participant;
import pse.trippy.tripservice.model.entity.Trip;
import pse.trippy.tripservice.model.enums.ParticipantRole;
import pse.trippy.tripservice.model.enums.ParticipantStatus;
import pse.trippy.tripservice.repository.ParticipantRepository;
import pse.trippy.tripservice.repository.TripRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final TripRepository tripRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public ParticipantActionResponse inviteParticipant(UUID tripId, InviteParticipantRequest request, UUID inviterId) {
        Trip trip = findTripOrThrow(tripId);
        ensureOwner(tripId, inviterId);

        if (participantRepository.existsByTripIdAndUserId(tripId, request.userId())) {
            throw new InvalidTripDataException("User is already a participant of this trip");
        }

        long currentCount = participantRepository.countByTripIdAndStatusIn(
                tripId, List.of(ParticipantStatus.INVITED, ParticipantStatus.ACCEPTED));
        if (currentCount >= trip.getMaxParticipants()) {
            throw new InvalidTripDataException("Trip has reached maximum number of participants");
        }

        Participant participant = Participant.builder()
                .trip(trip)
                .userId(request.userId())
                .role(ParticipantRole.MEMBER)
                .status(ParticipantStatus.INVITED)
                .build();
        participant = participantRepository.save(participant);

        publishEvent("trip.participant.invited", tripId, request.userId());

        return new ParticipantActionResponse("Participant invited successfully", toResponse(participant));
    }

    @Transactional
    public ParticipantActionResponse acceptInvite(UUID tripId, UUID userId) {
        findTripOrThrow(tripId);

        Participant participant = participantRepository.findByTripIdAndUserId(tripId, userId)
                .orElseThrow(() -> new InvalidTripDataException("No invitation found for this trip"));

        if (participant.getStatus() != ParticipantStatus.INVITED) {
            throw new InvalidTripDataException("Invitation is not in INVITED status");
        }

        participant.setStatus(ParticipantStatus.ACCEPTED);
        participant.setJoinedAt(Instant.now());
        participant = participantRepository.save(participant);

        publishEvent("trip.participant.joined", tripId, userId);

        return new ParticipantActionResponse("Invitation accepted successfully", toResponse(participant));
    }

    @Transactional
    public ParticipantActionResponse declineInvite(UUID tripId, UUID userId) {
        findTripOrThrow(tripId);

        Participant participant = participantRepository.findByTripIdAndUserId(tripId, userId)
                .orElseThrow(() -> new InvalidTripDataException("No invitation found for this trip"));

        if (participant.getStatus() != ParticipantStatus.INVITED) {
            throw new InvalidTripDataException("Invitation is not in INVITED status");
        }

        participant.setStatus(ParticipantStatus.DECLINED);
        participant = participantRepository.save(participant);

        return new ParticipantActionResponse("Invitation declined successfully", toResponse(participant));
    }

    @Transactional
    public void leaveTrip(UUID tripId, UUID userId) {
        findTripOrThrow(tripId);

        Participant participant = participantRepository.findByTripIdAndUserId(tripId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a participant of this trip"));

        if (participant.getRole() == ParticipantRole.OWNER) {
            throw new ForbiddenException("Owner cannot leave the trip. Delete the trip instead.");
        }

        participantRepository.delete(participant);

        publishEvent("trip.participant.left", tripId, userId);
    }

    @Transactional(readOnly = true)
    public List<ParticipantResponse> listParticipants(UUID tripId, UUID userId) {
        findTripOrThrow(tripId);

        // Only accepted participants can list participants
        participantRepository.findByTripIdAndUserId(tripId, userId)
                .filter(p -> p.getStatus() == ParticipantStatus.ACCEPTED)
                .orElseThrow(() -> new ForbiddenException("You are not a participant of this trip"));

        return participantRepository.findByTripId(tripId).stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private Trip findTripOrThrow(UUID tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
    }

    private void ensureOwner(UUID tripId, UUID userId) {
        participantRepository.findByTripIdAndUserId(tripId, userId)
                .filter(p -> p.getRole() == ParticipantRole.OWNER)
                .orElseThrow(() -> new ForbiddenException(
                        "Only the trip owner can perform this action"));
    }

    private ParticipantResponse toResponse(Participant p) {
        return new ParticipantResponse(
                p.getId(),
                p.getUserId(),
                p.getRole().name(),
                p.getStatus().name(),
                p.getJoinedAt()
        );
    }

    private void publishEvent(String routingKey, UUID tripId, UUID userId) {
        ParticipantEvent event = new ParticipantEvent(routingKey, tripId, userId, Instant.now());
        rabbitTemplate.convertAndSend(RabbitMQConfig.TRIP_EXCHANGE, routingKey, event);
    }
}
