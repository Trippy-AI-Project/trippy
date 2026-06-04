package pse.trippy.tripservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pse.trippy.tripservice.dto.request.CastVoteRequest;
import pse.trippy.tripservice.dto.request.VotingSettingsRequest;
import pse.trippy.tripservice.dto.response.ActivityVoteSummaryResponse;
import pse.trippy.tripservice.dto.response.VoteSummaryResponse;
import pse.trippy.tripservice.exception.ForbiddenException;
import pse.trippy.tripservice.exception.InvalidTripDataException;
import pse.trippy.tripservice.exception.TripNotFoundException;
import pse.trippy.tripservice.model.entity.Activity;
import pse.trippy.tripservice.model.entity.ActivityVote;
import pse.trippy.tripservice.model.entity.DayPlan;
import pse.trippy.tripservice.model.entity.DayPlanVote;
import pse.trippy.tripservice.model.entity.Itinerary;
import pse.trippy.tripservice.model.enums.ParticipantRole;
import pse.trippy.tripservice.model.enums.ParticipantStatus;
import pse.trippy.tripservice.model.enums.VoteType;
import pse.trippy.tripservice.repository.ActivityRepository;
import pse.trippy.tripservice.repository.ActivityVoteRepository;
import pse.trippy.tripservice.repository.DayPlanRepository;
import pse.trippy.tripservice.repository.DayPlanVoteRepository;
import pse.trippy.tripservice.repository.ItineraryRepository;
import pse.trippy.tripservice.repository.ParticipantRepository;
import pse.trippy.tripservice.repository.TripRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VotingService {

    private final TripRepository tripRepository;
    private final ItineraryRepository itineraryRepository;
    private final DayPlanRepository dayPlanRepository;
    private final DayPlanVoteRepository dayPlanVoteRepository;
    private final ActivityRepository activityRepository;
    private final ActivityVoteRepository activityVoteRepository;
    private final ParticipantRepository participantRepository;

    @Transactional
    public VoteSummaryResponse castVote(UUID tripId, int dayNumber, CastVoteRequest request, UUID userId) {
        ensureParticipant(tripId, userId);
        DayPlan dayPlan = findDayPlan(tripId, dayNumber);

        if (!dayPlan.isVotingEnabled()) {
            throw new InvalidTripDataException("Voting is not enabled for this day");
        }
        if (dayPlan.isVotingFrozen()) {
            throw new InvalidTripDataException("Voting is frozen for this day");
        }
        // Auto-freeze if deadline has passed
        if (dayPlan.getVotingDeadline() != null && Instant.now().isAfter(dayPlan.getVotingDeadline())) {
            dayPlan.setVotingFrozen(true);
            dayPlanRepository.save(dayPlan);
            throw new InvalidTripDataException("Voting deadline has passed");
        }

        VoteType voteType = parseVoteType(request.voteType());

        // Upsert vote
        DayPlanVote vote = dayPlanVoteRepository.findByDayPlanIdAndUserId(dayPlan.getId(), userId)
                .map(existing -> {
                    existing.setVoteType(voteType);
                    return existing;
                })
                .orElseGet(() -> DayPlanVote.builder()
                        .dayPlan(dayPlan)
                        .userId(userId)
                        .voteType(voteType)
                        .build());
        dayPlanVoteRepository.save(vote);

        // Check if majority reached → auto-freeze
        checkAndFreeze(dayPlan, tripId);

        return buildVoteSummary(dayPlan, userId);
    }

    @Transactional
    public VoteSummaryResponse removeVote(UUID tripId, int dayNumber, UUID userId) {
        ensureParticipant(tripId, userId);
        DayPlan dayPlan = findDayPlan(tripId, dayNumber);

        if (dayPlan.isVotingFrozen()) {
            throw new InvalidTripDataException("Voting is frozen for this day");
        }

        dayPlanVoteRepository.deleteByDayPlanIdAndUserId(dayPlan.getId(), userId);
        return buildVoteSummary(dayPlan, userId);
    }

    @Transactional(readOnly = true)
    public VoteSummaryResponse getVoteSummary(UUID tripId, int dayNumber, UUID userId) {
        ensureParticipant(tripId, userId);
        DayPlan dayPlan = findDayPlan(tripId, dayNumber);
        return buildVoteSummary(dayPlan, userId);
    }

    @Transactional
    public List<VoteSummaryResponse> updateVotingSettings(UUID tripId, VotingSettingsRequest request, UUID userId) {
        ensureOwnerOrEditor(tripId, userId);

        tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        Itinerary itinerary = itineraryRepository.findByTripId(tripId)
                .orElseThrow(() -> new InvalidTripDataException(
                        "No itinerary exists for this trip. Save the itinerary first."));

        List<DayPlan> dayPlans = dayPlanRepository.findByItineraryIdOrderByDayNumberAsc(itinerary.getId());

        for (DayPlan dp : dayPlans) {
            dp.setVotingEnabled(request.votingEnabled());
            dp.setVotingDeadline(request.votingDeadline());
            // If disabling voting, also unfreeze
            if (!request.votingEnabled()) {
                dp.setVotingFrozen(false);
            }
        }
        dayPlanRepository.saveAll(dayPlans);

        return dayPlans.stream()
                .map(dp -> buildVoteSummary(dp, userId))
                .toList();
    }

    // ── Activity Voting ────────────────────────────────────────────────

    @Transactional
    public ActivityVoteSummaryResponse castActivityVote(UUID tripId, UUID activityId, CastVoteRequest request, UUID userId) {
        ensureParticipant(tripId, userId);
        Activity activity = findActivityInTrip(tripId, activityId);

        // Check if day-level voting is enabled
        DayPlan dayPlan = activity.getDayPlan();
        if (!dayPlan.isVotingEnabled()) {
            throw new InvalidTripDataException("Voting is not enabled for this day");
        }
        if (dayPlan.isVotingFrozen()) {
            throw new InvalidTripDataException("Voting is frozen for this day");
        }

        VoteType voteType = parseVoteType(request.voteType());

        ActivityVote vote = activityVoteRepository.findByActivityIdAndUserId(activityId, userId)
                .map(existing -> {
                    existing.setVoteType(voteType);
                    return existing;
                })
                .orElseGet(() -> ActivityVote.builder()
                        .activity(activity)
                        .userId(userId)
                        .voteType(voteType)
                        .build());
        activityVoteRepository.save(vote);

        return buildActivityVoteSummary(activityId, userId);
    }

    @Transactional
    public ActivityVoteSummaryResponse removeActivityVote(UUID tripId, UUID activityId, UUID userId) {
        ensureParticipant(tripId, userId);
        Activity activity = findActivityInTrip(tripId, activityId);

        DayPlan dayPlan = activity.getDayPlan();
        if (dayPlan.isVotingFrozen()) {
            throw new InvalidTripDataException("Voting is frozen for this day");
        }

        activityVoteRepository.deleteByActivityIdAndUserId(activityId, userId);
        return buildActivityVoteSummary(activityId, userId);
    }

    @Transactional(readOnly = true)
    public ActivityVoteSummaryResponse getActivityVoteSummary(UUID tripId, UUID activityId, UUID userId) {
        ensureParticipant(tripId, userId);
        findActivityInTrip(tripId, activityId);
        return buildActivityVoteSummary(activityId, userId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private DayPlan findDayPlan(UUID tripId, int dayNumber) {
        // Verify trip exists first
        tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        Itinerary itinerary = itineraryRepository.findByTripId(tripId)
                .orElseThrow(() -> new InvalidTripDataException(
                        "No itinerary exists for this trip. Save the itinerary first."));

        return dayPlanRepository.findByItineraryIdAndDayNumber(itinerary.getId(), dayNumber)
                .orElseThrow(() -> new InvalidTripDataException("Day " + dayNumber + " not found"));
    }

    private void checkAndFreeze(DayPlan dayPlan, UUID tripId) {
        long totalParticipants = participantRepository.countByTripIdAndStatusIn(
                tripId, List.of(ParticipantStatus.ACCEPTED));
        long totalVotes = dayPlanVoteRepository.findByDayPlanId(dayPlan.getId()).size();

        // Freeze when all participants have voted (majority = >50%)
        if (totalParticipants > 0 && totalVotes >= totalParticipants) {
            dayPlan.setVotingFrozen(true);
            dayPlanRepository.save(dayPlan);
        }
    }

    private VoteSummaryResponse buildVoteSummary(DayPlan dayPlan, UUID userId) {
        long upvotes = dayPlanVoteRepository.countByDayPlanIdAndVoteType(dayPlan.getId(), VoteType.UPVOTE);
        long downvotes = dayPlanVoteRepository.countByDayPlanIdAndVoteType(dayPlan.getId(), VoteType.DOWNVOTE);

        String currentUserVote = dayPlanVoteRepository.findByDayPlanIdAndUserId(dayPlan.getId(), userId)
                .map(v -> v.getVoteType().name())
                .orElse(null);

        return new VoteSummaryResponse(
                dayPlan.getId(),
                dayPlan.getDayNumber(),
                upvotes,
                downvotes,
                currentUserVote,
                dayPlan.isVotingEnabled(),
                dayPlan.isVotingFrozen(),
                dayPlan.getVotingDeadline()
        );
    }

    private VoteType parseVoteType(String voteType) {
        try {
            return VoteType.valueOf(voteType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidTripDataException("Invalid vote type: " + voteType + ". Must be UPVOTE or DOWNVOTE");
        }
    }

    private void ensureParticipant(UUID tripId, UUID userId) {
        participantRepository.findByTripIdAndUserId(tripId, userId)
                .filter(p -> p.getStatus() == ParticipantStatus.ACCEPTED)
                .orElseThrow(() -> new ForbiddenException(
                        "You are not a participant of this trip"));
    }

    private void ensureOwnerOrEditor(UUID tripId, UUID userId) {
        participantRepository.findByTripIdAndUserId(tripId, userId)
                .filter(p -> p.getStatus() == ParticipantStatus.ACCEPTED)
                .filter(p -> p.getRole() == ParticipantRole.OWNER
                        || p.getRole() == ParticipantRole.EDITOR)
                .orElseThrow(() -> new ForbiddenException(
                        "Only the trip owner or editor can modify voting settings"));
    }

    private Activity findActivityInTrip(UUID tripId, UUID activityId) {
        // Verify trip exists
        tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new InvalidTripDataException("Activity not found: " + activityId));

        // Verify the activity belongs to this trip's itinerary
        Itinerary itinerary = itineraryRepository.findByTripId(tripId)
                .orElseThrow(() -> new InvalidTripDataException("No itinerary exists for this trip"));

        DayPlan dayPlan = activity.getDayPlan();
        if (!dayPlan.getItinerary().getId().equals(itinerary.getId())) {
            throw new InvalidTripDataException("Activity does not belong to this trip");
        }

        return activity;
    }

    private ActivityVoteSummaryResponse buildActivityVoteSummary(UUID activityId, UUID userId) {
        long upvotes = activityVoteRepository.countByActivityIdAndVoteType(activityId, VoteType.UPVOTE);
        long downvotes = activityVoteRepository.countByActivityIdAndVoteType(activityId, VoteType.DOWNVOTE);

        String currentUserVote = activityVoteRepository.findByActivityIdAndUserId(activityId, userId)
                .map(v -> v.getVoteType().name())
                .orElse(null);

        return new ActivityVoteSummaryResponse(activityId, upvotes, downvotes, currentUserVote);
    }
}
