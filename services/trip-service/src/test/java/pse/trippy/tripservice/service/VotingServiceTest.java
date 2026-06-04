package pse.trippy.tripservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import pse.trippy.tripservice.model.entity.Participant;
import pse.trippy.tripservice.model.entity.Trip;
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
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VotingService")
class VotingServiceTest {

    @Mock
    private TripRepository tripRepository;
    @Mock
    private ItineraryRepository itineraryRepository;
    @Mock
    private DayPlanRepository dayPlanRepository;
    @Mock
    private DayPlanVoteRepository dayPlanVoteRepository;
    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private ActivityVoteRepository activityVoteRepository;
    @Mock
    private ParticipantRepository participantRepository;

    @InjectMocks
    private VotingService votingService;

    private static final UUID TRIP_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ITINERARY_ID = UUID.randomUUID();
    private static final UUID DAY_PLAN_ID = UUID.randomUUID();
    private static final UUID ACTIVITY_ID = UUID.randomUUID();

    private Trip trip;
    private Itinerary itinerary;
    private DayPlan dayPlan;
    private Activity activity;

    @BeforeEach
    void setUp() {
        trip = Trip.builder()
                .title("Test Trip")
                .destination("Barcelona")
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 7, 7))
                .createdBy(USER_ID)
                .build();
        trip.setId(TRIP_ID);

        itinerary = Itinerary.builder().trip(trip).build();
        itinerary.setId(ITINERARY_ID);

        dayPlan = DayPlan.builder()
                .itinerary(itinerary)
                .dayNumber(1)
                .votingEnabled(true)
                .votingFrozen(false)
                .build();
        dayPlan.setId(DAY_PLAN_ID);

        activity = Activity.builder()
                .dayPlan(dayPlan)
                .title("Visit Museum")
                .build();
        activity.setId(ACTIVITY_ID);
    }

    private void stubParticipant() {
        Participant p = Participant.builder()
                .trip(trip)
                .userId(USER_ID)
                .role(ParticipantRole.MEMBER)
                .status(ParticipantStatus.ACCEPTED)
                .build();
        when(participantRepository.findByTripIdAndUserId(TRIP_ID, USER_ID))
                .thenReturn(Optional.of(p));
    }

    private void stubOwner() {
        Participant p = Participant.builder()
                .trip(trip)
                .userId(USER_ID)
                .role(ParticipantRole.OWNER)
                .status(ParticipantStatus.ACCEPTED)
                .build();
        when(participantRepository.findByTripIdAndUserId(TRIP_ID, USER_ID))
                .thenReturn(Optional.of(p));
    }

    private void stubDayPlanLookup() {
        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(itineraryRepository.findByTripId(TRIP_ID)).thenReturn(Optional.of(itinerary));
        when(dayPlanRepository.findByItineraryIdAndDayNumber(ITINERARY_ID, 1))
                .thenReturn(Optional.of(dayPlan));
    }

    // =========================================================================
    // castVote (day plan)
    // =========================================================================

    @Nested
    @DisplayName("castVote")
    class CastVote {

        @Test
        @DisplayName("participant can cast an upvote on a day plan")
        void canCastUpvote() {
            stubParticipant();
            stubDayPlanLookup();

            when(dayPlanVoteRepository.findByDayPlanIdAndUserId(DAY_PLAN_ID, USER_ID))
                    .thenReturn(Optional.empty());
            when(dayPlanVoteRepository.save(any(DayPlanVote.class))).thenAnswer(i -> i.getArgument(0));
            when(participantRepository.countByTripIdAndStatusIn(eq(TRIP_ID), any(Collection.class)))
                    .thenReturn(5L);
            when(dayPlanVoteRepository.findByDayPlanId(DAY_PLAN_ID))
                    .thenReturn(List.of(DayPlanVote.builder().dayPlan(dayPlan).userId(USER_ID).voteType(VoteType.UPVOTE).build()));
            when(dayPlanVoteRepository.countByDayPlanIdAndVoteType(DAY_PLAN_ID, VoteType.UPVOTE))
                    .thenReturn(1L);
            when(dayPlanVoteRepository.countByDayPlanIdAndVoteType(DAY_PLAN_ID, VoteType.DOWNVOTE))
                    .thenReturn(0L);

            VoteSummaryResponse response = votingService.castVote(TRIP_ID, 1, new CastVoteRequest("UPVOTE"), USER_ID);

            assertThat(response.dayPlanId()).isEqualTo(DAY_PLAN_ID);
            assertThat(response.upvotes()).isEqualTo(1);
            assertThat(response.downvotes()).isEqualTo(0);
            assertThat(response.votingEnabled()).isTrue();
            assertThat(response.votingFrozen()).isFalse();
            verify(dayPlanVoteRepository).save(any(DayPlanVote.class));
        }

        @Test
        @DisplayName("throws when voting is not enabled")
        void throwsWhenVotingDisabled() {
            dayPlan.setVotingEnabled(false);
            stubParticipant();
            stubDayPlanLookup();

            assertThatThrownBy(() -> votingService.castVote(TRIP_ID, 1, new CastVoteRequest("UPVOTE"), USER_ID))
                    .isInstanceOf(InvalidTripDataException.class)
                    .hasMessageContaining("not enabled");
        }

        @Test
        @DisplayName("throws when voting is frozen")
        void throwsWhenVotingFrozen() {
            dayPlan.setVotingFrozen(true);
            stubParticipant();
            stubDayPlanLookup();

            assertThatThrownBy(() -> votingService.castVote(TRIP_ID, 1, new CastVoteRequest("DOWNVOTE"), USER_ID))
                    .isInstanceOf(InvalidTripDataException.class)
                    .hasMessageContaining("frozen");
        }

        @Test
        @DisplayName("throws for invalid vote type")
        void throwsForInvalidVoteType() {
            stubParticipant();
            stubDayPlanLookup();

            assertThatThrownBy(() -> votingService.castVote(TRIP_ID, 1, new CastVoteRequest("INVALID"), USER_ID))
                    .isInstanceOf(InvalidTripDataException.class)
                    .hasMessageContaining("Invalid vote type");
        }

        @Test
        @DisplayName("non-participant cannot vote")
        void nonParticipantCannotVote() {
            when(participantRepository.findByTripIdAndUserId(TRIP_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> votingService.castVote(TRIP_ID, 1, new CastVoteRequest("UPVOTE"), USER_ID))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("auto-freezes when deadline has passed")
        void autoFreezesOnDeadline() {
            dayPlan.setVotingDeadline(Instant.now().minusSeconds(3600)); // deadline 1h ago
            stubParticipant();
            stubDayPlanLookup();
            when(dayPlanRepository.save(any(DayPlan.class))).thenAnswer(i -> i.getArgument(0));

            assertThatThrownBy(() -> votingService.castVote(TRIP_ID, 1, new CastVoteRequest("UPVOTE"), USER_ID))
                    .isInstanceOf(InvalidTripDataException.class)
                    .hasMessageContaining("deadline has passed");
        }

        @Test
        @DisplayName("updates existing vote instead of creating duplicate")
        void updatesExistingVote() {
            stubParticipant();
            stubDayPlanLookup();

            DayPlanVote existing = DayPlanVote.builder()
                    .dayPlan(dayPlan)
                    .userId(USER_ID)
                    .voteType(VoteType.UPVOTE)
                    .build();
            when(dayPlanVoteRepository.findByDayPlanIdAndUserId(DAY_PLAN_ID, USER_ID))
                    .thenReturn(Optional.of(existing));
            when(dayPlanVoteRepository.save(any(DayPlanVote.class))).thenAnswer(i -> i.getArgument(0));
            when(participantRepository.countByTripIdAndStatusIn(eq(TRIP_ID), any(Collection.class)))
                    .thenReturn(5L);
            when(dayPlanVoteRepository.findByDayPlanId(DAY_PLAN_ID)).thenReturn(List.of(existing));
            when(dayPlanVoteRepository.countByDayPlanIdAndVoteType(DAY_PLAN_ID, VoteType.UPVOTE)).thenReturn(0L);
            when(dayPlanVoteRepository.countByDayPlanIdAndVoteType(DAY_PLAN_ID, VoteType.DOWNVOTE)).thenReturn(1L);

            VoteSummaryResponse response = votingService.castVote(TRIP_ID, 1, new CastVoteRequest("DOWNVOTE"), USER_ID);

            assertThat(existing.getVoteType()).isEqualTo(VoteType.DOWNVOTE);
            assertThat(response.downvotes()).isEqualTo(1);
        }
    }

    // =========================================================================
    // removeVote
    // =========================================================================

    @Nested
    @DisplayName("removeVote")
    class RemoveVote {

        @Test
        @DisplayName("participant can remove their vote")
        void canRemoveVote() {
            stubParticipant();
            stubDayPlanLookup();
            when(dayPlanVoteRepository.countByDayPlanIdAndVoteType(DAY_PLAN_ID, VoteType.UPVOTE)).thenReturn(0L);
            when(dayPlanVoteRepository.countByDayPlanIdAndVoteType(DAY_PLAN_ID, VoteType.DOWNVOTE)).thenReturn(0L);
            when(dayPlanVoteRepository.findByDayPlanIdAndUserId(DAY_PLAN_ID, USER_ID))
                    .thenReturn(Optional.empty());

            VoteSummaryResponse response = votingService.removeVote(TRIP_ID, 1, USER_ID);

            verify(dayPlanVoteRepository).deleteByDayPlanIdAndUserId(DAY_PLAN_ID, USER_ID);
            assertThat(response.upvotes()).isEqualTo(0);
        }

        @Test
        @DisplayName("throws when voting is frozen")
        void throwsWhenFrozen() {
            dayPlan.setVotingFrozen(true);
            stubParticipant();
            stubDayPlanLookup();

            assertThatThrownBy(() -> votingService.removeVote(TRIP_ID, 1, USER_ID))
                    .isInstanceOf(InvalidTripDataException.class)
                    .hasMessageContaining("frozen");
        }
    }

    // =========================================================================
    // getVoteSummary
    // =========================================================================

    @Nested
    @DisplayName("getVoteSummary")
    class GetVoteSummary {

        @Test
        @DisplayName("returns accurate vote counts and current user vote")
        void returnsVoteSummary() {
            stubParticipant();
            stubDayPlanLookup();
            when(dayPlanVoteRepository.countByDayPlanIdAndVoteType(DAY_PLAN_ID, VoteType.UPVOTE)).thenReturn(3L);
            when(dayPlanVoteRepository.countByDayPlanIdAndVoteType(DAY_PLAN_ID, VoteType.DOWNVOTE)).thenReturn(1L);

            DayPlanVote userVote = DayPlanVote.builder()
                    .dayPlan(dayPlan).userId(USER_ID).voteType(VoteType.UPVOTE).build();
            when(dayPlanVoteRepository.findByDayPlanIdAndUserId(DAY_PLAN_ID, USER_ID))
                    .thenReturn(Optional.of(userVote));

            VoteSummaryResponse response = votingService.getVoteSummary(TRIP_ID, 1, USER_ID);

            assertThat(response.upvotes()).isEqualTo(3);
            assertThat(response.downvotes()).isEqualTo(1);
            assertThat(response.currentUserVote()).isEqualTo("UPVOTE");
            assertThat(response.dayNumber()).isEqualTo(1);
        }
    }

    // =========================================================================
    // updateVotingSettings
    // =========================================================================

    @Nested
    @DisplayName("updateVotingSettings")
    class UpdateVotingSettings {

        @Test
        @DisplayName("owner can enable voting for all days")
        void ownerCanEnableVoting() {
            stubOwner();
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
            when(itineraryRepository.findByTripId(TRIP_ID)).thenReturn(Optional.of(itinerary));

            DayPlan dp1 = DayPlan.builder().itinerary(itinerary).dayNumber(1).votingEnabled(false).build();
            dp1.setId(UUID.randomUUID());
            DayPlan dp2 = DayPlan.builder().itinerary(itinerary).dayNumber(2).votingEnabled(false).build();
            dp2.setId(UUID.randomUUID());
            when(dayPlanRepository.findByItineraryIdOrderByDayNumberAsc(ITINERARY_ID))
                    .thenReturn(List.of(dp1, dp2));
            when(dayPlanRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
            when(dayPlanVoteRepository.countByDayPlanIdAndVoteType(any(), eq(VoteType.UPVOTE))).thenReturn(0L);
            when(dayPlanVoteRepository.countByDayPlanIdAndVoteType(any(), eq(VoteType.DOWNVOTE))).thenReturn(0L);
            when(dayPlanVoteRepository.findByDayPlanIdAndUserId(any(), eq(USER_ID))).thenReturn(Optional.empty());

            Instant deadline = Instant.now().plusSeconds(86400);
            List<VoteSummaryResponse> responses = votingService.updateVotingSettings(
                    TRIP_ID, new VotingSettingsRequest(true, deadline), USER_ID);

            assertThat(responses).hasSize(2);
            assertThat(dp1.isVotingEnabled()).isTrue();
            assertThat(dp2.isVotingEnabled()).isTrue();
            assertThat(dp1.getVotingDeadline()).isEqualTo(deadline);
        }

        @Test
        @DisplayName("non-owner/editor cannot change voting settings")
        void nonOwnerCannotChange() {
            Participant member = Participant.builder()
                    .trip(trip).userId(USER_ID)
                    .role(ParticipantRole.MEMBER)
                    .status(ParticipantStatus.ACCEPTED).build();
            when(participantRepository.findByTripIdAndUserId(TRIP_ID, USER_ID))
                    .thenReturn(Optional.of(member));

            assertThatThrownBy(() -> votingService.updateVotingSettings(
                    TRIP_ID, new VotingSettingsRequest(true, null), USER_ID))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("owner or editor");
        }

        @Test
        @DisplayName("throws when no itinerary exists")
        void throwsWhenNoItinerary() {
            stubOwner();
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
            when(itineraryRepository.findByTripId(TRIP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> votingService.updateVotingSettings(
                    TRIP_ID, new VotingSettingsRequest(true, null), USER_ID))
                    .isInstanceOf(InvalidTripDataException.class)
                    .hasMessageContaining("No itinerary");
        }
    }

    // =========================================================================
    // castActivityVote
    // =========================================================================

    @Nested
    @DisplayName("castActivityVote")
    class CastActivityVote {

        @Test
        @DisplayName("participant can vote on an activity")
        void canVoteOnActivity() {
            stubParticipant();
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
            when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));
            when(itineraryRepository.findByTripId(TRIP_ID)).thenReturn(Optional.of(itinerary));

            when(activityVoteRepository.findByActivityIdAndUserId(ACTIVITY_ID, USER_ID))
                    .thenReturn(Optional.empty());
            when(activityVoteRepository.save(any(ActivityVote.class))).thenAnswer(i -> i.getArgument(0));
            when(activityVoteRepository.countByActivityIdAndVoteType(ACTIVITY_ID, VoteType.UPVOTE)).thenReturn(1L);
            when(activityVoteRepository.countByActivityIdAndVoteType(ACTIVITY_ID, VoteType.DOWNVOTE)).thenReturn(0L);
            when(activityVoteRepository.findByActivityIdAndUserId(ACTIVITY_ID, USER_ID))
                    .thenReturn(Optional.empty());

            ActivityVoteSummaryResponse response = votingService.castActivityVote(
                    TRIP_ID, ACTIVITY_ID, new CastVoteRequest("UPVOTE"), USER_ID);

            assertThat(response.activityId()).isEqualTo(ACTIVITY_ID);
            assertThat(response.upvotes()).isEqualTo(1);
        }

        @Test
        @DisplayName("throws when voting is not enabled for the day")
        void throwsWhenDayVotingDisabled() {
            dayPlan.setVotingEnabled(false);
            stubParticipant();
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
            when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));
            when(itineraryRepository.findByTripId(TRIP_ID)).thenReturn(Optional.of(itinerary));

            assertThatThrownBy(() -> votingService.castActivityVote(
                    TRIP_ID, ACTIVITY_ID, new CastVoteRequest("UPVOTE"), USER_ID))
                    .isInstanceOf(InvalidTripDataException.class)
                    .hasMessageContaining("not enabled");
        }

        @Test
        @DisplayName("throws when voting is frozen for the day")
        void throwsWhenDayVotingFrozen() {
            dayPlan.setVotingFrozen(true);
            stubParticipant();
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
            when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));
            when(itineraryRepository.findByTripId(TRIP_ID)).thenReturn(Optional.of(itinerary));

            assertThatThrownBy(() -> votingService.castActivityVote(
                    TRIP_ID, ACTIVITY_ID, new CastVoteRequest("DOWNVOTE"), USER_ID))
                    .isInstanceOf(InvalidTripDataException.class)
                    .hasMessageContaining("frozen");
        }
    }

    // =========================================================================
    // removeActivityVote
    // =========================================================================

    @Nested
    @DisplayName("removeActivityVote")
    class RemoveActivityVote {

        @Test
        @DisplayName("participant can remove their activity vote")
        void canRemoveActivityVote() {
            stubParticipant();
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
            when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));
            when(itineraryRepository.findByTripId(TRIP_ID)).thenReturn(Optional.of(itinerary));
            when(activityVoteRepository.countByActivityIdAndVoteType(ACTIVITY_ID, VoteType.UPVOTE)).thenReturn(0L);
            when(activityVoteRepository.countByActivityIdAndVoteType(ACTIVITY_ID, VoteType.DOWNVOTE)).thenReturn(0L);
            when(activityVoteRepository.findByActivityIdAndUserId(ACTIVITY_ID, USER_ID))
                    .thenReturn(Optional.empty());

            ActivityVoteSummaryResponse response = votingService.removeActivityVote(TRIP_ID, ACTIVITY_ID, USER_ID);

            verify(activityVoteRepository).deleteByActivityIdAndUserId(ACTIVITY_ID, USER_ID);
            assertThat(response.upvotes()).isEqualTo(0);
        }

        @Test
        @DisplayName("throws when voting is frozen")
        void throwsWhenFrozen() {
            dayPlan.setVotingFrozen(true);
            stubParticipant();
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
            when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(activity));
            when(itineraryRepository.findByTripId(TRIP_ID)).thenReturn(Optional.of(itinerary));

            assertThatThrownBy(() -> votingService.removeActivityVote(TRIP_ID, ACTIVITY_ID, USER_ID))
                    .isInstanceOf(InvalidTripDataException.class)
                    .hasMessageContaining("frozen");
        }
    }

    // =========================================================================
    // Edge cases / Trip not found
    // =========================================================================

    @Nested
    @DisplayName("trip not found scenarios")
    class TripNotFound {

        @Test
        @DisplayName("castVote throws TripNotFoundException for unknown trip")
        void castVoteThrows() {
            stubParticipant();
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> votingService.castVote(TRIP_ID, 1, new CastVoteRequest("UPVOTE"), USER_ID))
                    .isInstanceOf(TripNotFoundException.class);
        }

        @Test
        @DisplayName("getVoteSummary throws for missing itinerary")
        void getVoteSummaryThrowsNoItinerary() {
            stubParticipant();
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
            when(itineraryRepository.findByTripId(TRIP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> votingService.getVoteSummary(TRIP_ID, 1, USER_ID))
                    .isInstanceOf(InvalidTripDataException.class)
                    .hasMessageContaining("No itinerary");
        }

        @Test
        @DisplayName("getVoteSummary throws for missing day plan")
        void getVoteSummaryThrowsMissingDay() {
            stubParticipant();
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
            when(itineraryRepository.findByTripId(TRIP_ID)).thenReturn(Optional.of(itinerary));
            when(dayPlanRepository.findByItineraryIdAndDayNumber(ITINERARY_ID, 99))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> votingService.getVoteSummary(TRIP_ID, 99, USER_ID))
                    .isInstanceOf(InvalidTripDataException.class)
                    .hasMessageContaining("Day 99 not found");
        }
    }
}
