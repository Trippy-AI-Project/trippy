package pse.trippy.tripservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParticipantService")
class ParticipantServiceTest {

    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ParticipantService participantService;

    private static final UUID TRIP_ID = UUID.randomUUID();
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID INVITEE_ID = UUID.randomUUID();
    private Trip trip;

    @BeforeEach
    void setUp() {
        trip = Trip.builder()
                .title("Test Trip")
                .destination("Barcelona")
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 7, 7))
                .maxParticipants(10)
                .createdBy(OWNER_ID)
                .build();
        trip.setId(TRIP_ID);
    }

    private Participant ownerParticipant() {
        Participant p = Participant.builder()
                .trip(trip)
                .userId(OWNER_ID)
                .role(ParticipantRole.OWNER)
                .status(ParticipantStatus.ACCEPTED)
                .build();
        p.setId(UUID.randomUUID());
        return p;
    }

    private Participant invitedParticipant() {
        Participant p = Participant.builder()
                .trip(trip)
                .userId(INVITEE_ID)
                .role(ParticipantRole.MEMBER)
                .status(ParticipantStatus.INVITED)
                .build();
        p.setId(UUID.randomUUID());
        return p;
    }

    private Participant acceptedMember() {
        Participant p = Participant.builder()
                .trip(trip)
                .userId(INVITEE_ID)
                .role(ParticipantRole.MEMBER)
                .status(ParticipantStatus.ACCEPTED)
                .build();
        p.setId(UUID.randomUUID());
        return p;
    }

    // =========================================================================
    // inviteParticipant
    // =========================================================================

    @Nested
    @DisplayName("inviteParticipant")
    class InviteParticipant {

        @Test
        @DisplayName("owner can invite a new participant")
        void ownerCanInvite() {
            InviteParticipantRequest request = new InviteParticipantRequest(INVITEE_ID);

            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
            when(participantRepository.findByTripIdAndUserId(TRIP_ID, OWNER_ID))
                    .thenReturn(Optional.of(ownerParticipant()));
            when(participantRepository.existsByTripIdAndUserId(TRIP_ID, INVITEE_ID)).thenReturn(false);
            when(participantRepository.countByTripIdAndStatusIn(eq(TRIP_ID), any(Collection.class))).thenReturn(1L);
            when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> {
                Participant p = invocation.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });

            ParticipantActionResponse response = participantService.inviteParticipant(TRIP_ID, request, OWNER_ID);

            assertThat(response.message()).isEqualTo("Participant invited successfully");
            assertThat(response.participant().userId()).isEqualTo(INVITEE_ID);
            assertThat(response.participant().role()).isEqualTo("MEMBER");
            assertThat(response.participant().status()).isEqualTo("INVITED");
            verify(rabbitTemplate).convertAndSend(anyString(), eq("trip.participant.invited"), any());
        }

        @Test
        @DisplayName("throws TripNotFoundException for unknown trip")
        void throwsForUnknownTrip() {
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> participantService.inviteParticipant(
                    TRIP_ID, new InviteParticipantRequest(INVITEE_ID), OWNER_ID))
                    .isInstanceOf(TripNotFoundException.class);
        }

        @Test
        @DisplayName("non-owner cannot invite")
        void nonOwnerCannotInvite() {
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
            when(participantRepository.findByTripIdAndUserId(TRIP_ID, INVITEE_ID))
                    .thenReturn(Optional.of(invitedParticipant()));

            assertThatThrownBy(() -> participantService.inviteParticipant(
                    TRIP_ID, new InviteParticipantRequest(UUID.randomUUID()), INVITEE_ID))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("throws when user is already a participant")
        void throwsForDuplicateParticipant() {
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
            when(participantRepository.findByTripIdAndUserId(TRIP_ID, OWNER_ID))
                    .thenReturn(Optional.of(ownerParticipant()));
            when(participantRepository.existsByTripIdAndUserId(TRIP_ID, INVITEE_ID)).thenReturn(true);

            assertThatThrownBy(() -> participantService.inviteParticipant(
                    TRIP_ID, new InviteParticipantRequest(INVITEE_ID), OWNER_ID))
                    .isInstanceOf(InvalidTripDataException.class)
                    .hasMessageContaining("already a participant");
        }

        @Test
        @DisplayName("throws when max participants reached")
        void throwsWhenMaxReached() {
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
            when(participantRepository.findByTripIdAndUserId(TRIP_ID, OWNER_ID))
                    .thenReturn(Optional.of(ownerParticipant()));
            when(participantRepository.existsByTripIdAndUserId(TRIP_ID, INVITEE_ID)).thenReturn(false);
            when(participantRepository.countByTripIdAndStatusIn(eq(TRIP_ID), any(Collection.class))).thenReturn(10L);

            assertThatThrownBy(() -> participantService.inviteParticipant(
                    TRIP_ID, new InviteParticipantRequest(INVITEE_ID), OWNER_ID))
                    .isInstanceOf(InvalidTripDataException.class)
                    .hasMessageContaining("maximum");
        }
    }

    // =========================================================================
    // acceptInvite
    // =========================================================================

    @Nested
    @DisplayName("acceptInvite")
    class AcceptInvite {

        @Test
        @DisplayName("invitee can accept an invitation")
        void inviteeCanAccept() {
            Participant invited = invitedParticipant();

            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
            when(participantRepository.findByTripIdAndUserId(TRIP_ID, INVITEE_ID))
                    .thenReturn(Optional.of(invited));
            when(participantRepository.save(any(Participant.class))).thenAnswer(i -> i.getArgument(0));

            ParticipantActionResponse response = participantService.acceptInvite(TRIP_ID, INVITEE_ID);

            assertThat(response.message()).isEqualTo("Invitation accepted successfully");
            assertThat(response.participant().status()).isEqualTo("ACCEPTED");
            assertThat(response.participant().joinedAt()).isNotNull();
            verify(rabbitTemplate).convertAndSend(anyString(), eq("trip.participant.joined"), any());
        }

        @Test
        @DisplayName("throws when no invitation exists")
        void throwsWhenNoInvitation() {
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
            when(participantRepository.findByTripIdAndUserId(TRIP_ID, INVITEE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> participantService.acceptInvite(TRIP_ID, INVITEE_ID))
                    .isInstanceOf(InvalidTripDataException.class);
        }

        @Test
        @DisplayName("throws when invitation is not in INVITED status")
        void throwsWhenNotInvited() {
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
            when(participantRepository.findByTripIdAndUserId(TRIP_ID, INVITEE_ID))
                    .thenReturn(Optional.of(acceptedMember()));

            assertThatThrownBy(() -> participantService.acceptInvite(TRIP_ID, INVITEE_ID))
                    .isInstanceOf(InvalidTripDataException.class)
                    .hasMessageContaining("not in INVITED status");
        }
    }

    // =========================================================================
    // declineInvite
    // =========================================================================

    @Nested
    @DisplayName("declineInvite")
    class DeclineInvite {

        @Test
        @DisplayName("invitee can decline an invitation")
        void inviteeCanDecline() {
            Participant invited = invitedParticipant();

            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
            when(participantRepository.findByTripIdAndUserId(TRIP_ID, INVITEE_ID))
                    .thenReturn(Optional.of(invited));
            when(participantRepository.save(any(Participant.class))).thenAnswer(i -> i.getArgument(0));

            ParticipantActionResponse response = participantService.declineInvite(TRIP_ID, INVITEE_ID);

            assertThat(response.message()).isEqualTo("Invitation declined successfully");
            assertThat(response.participant().status()).isEqualTo("DECLINED");
        }

        @Test
        @DisplayName("throws when invitation is not in INVITED status")
        void throwsWhenNotInvited() {
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
            when(participantRepository.findByTripIdAndUserId(TRIP_ID, INVITEE_ID))
                    .thenReturn(Optional.of(acceptedMember()));

            assertThatThrownBy(() -> participantService.declineInvite(TRIP_ID, INVITEE_ID))
                    .isInstanceOf(InvalidTripDataException.class);
        }
    }

    // =========================================================================
    // leaveTrip
    // =========================================================================

    @Nested
    @DisplayName("leaveTrip")
    class LeaveTrip {

        @Test
        @DisplayName("member can leave the trip")
        void memberCanLeave() {
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
            when(participantRepository.findByTripIdAndUserId(TRIP_ID, INVITEE_ID))
                    .thenReturn(Optional.of(acceptedMember()));

            participantService.leaveTrip(TRIP_ID, INVITEE_ID);

            verify(participantRepository).delete(any(Participant.class));
            verify(rabbitTemplate).convertAndSend(anyString(), eq("trip.participant.left"), any());
        }

        @Test
        @DisplayName("owner cannot leave the trip")
        void ownerCannotLeave() {
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
            when(participantRepository.findByTripIdAndUserId(TRIP_ID, OWNER_ID))
                    .thenReturn(Optional.of(ownerParticipant()));

            assertThatThrownBy(() -> participantService.leaveTrip(TRIP_ID, OWNER_ID))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Owner cannot leave");

            verify(participantRepository, never()).delete(any(Participant.class));
        }

        @Test
        @DisplayName("throws when user is not a participant")
        void throwsWhenNotParticipant() {
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
            when(participantRepository.findByTripIdAndUserId(TRIP_ID, INVITEE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> participantService.leaveTrip(TRIP_ID, INVITEE_ID))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // =========================================================================
    // listParticipants
    // =========================================================================

    @Nested
    @DisplayName("listParticipants")
    class ListParticipants {

        @Test
        @DisplayName("accepted participant can list participants")
        void acceptedCanList() {
            Participant owner = ownerParticipant();
            Participant member = acceptedMember();

            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
            when(participantRepository.findByTripIdAndUserId(TRIP_ID, INVITEE_ID))
                    .thenReturn(Optional.of(member));
            when(participantRepository.findByTripId(TRIP_ID))
                    .thenReturn(List.of(owner, member));

            List<ParticipantResponse> result = participantService.listParticipants(TRIP_ID, INVITEE_ID);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("non-participant cannot list participants")
        void nonParticipantCannotList() {
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
            when(participantRepository.findByTripIdAndUserId(TRIP_ID, INVITEE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> participantService.listParticipants(TRIP_ID, INVITEE_ID))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("throws TripNotFoundException for unknown trip")
        void throwsForUnknownTrip() {
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> participantService.listParticipants(TRIP_ID, INVITEE_ID))
                    .isInstanceOf(TripNotFoundException.class);
        }
    }
}
