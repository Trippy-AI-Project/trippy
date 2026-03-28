package pse.trippy.tripservice.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pse.trippy.tripservice.model.enums.ParticipantRole;
import pse.trippy.tripservice.model.enums.ParticipantStatus;
import pse.trippy.tripservice.model.enums.TripStatus;
import pse.trippy.tripservice.model.enums.TripVisibility;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link Participant} entity.
 */
@DisplayName("Participant entity")
class ParticipantTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private Trip buildTrip() {
        return Trip.builder()
                .title("Test Trip")
                .destination("Berlin")
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 7, 10))
                .createdBy(UUID.randomUUID())
                .build();
    }

    // =========================================================================
    // Default values
    // =========================================================================

    @Nested
    @DisplayName("Default field values")
    class DefaultValues {

        @Test
        @DisplayName("role defaults to VIEWER when not set")
        void roleDefaultsToViewer() {
            Participant p = Participant.builder().trip(buildTrip()).userId(USER_ID).build();
            assertThat(p.getRole()).isEqualTo(ParticipantRole.VIEWER);
        }

        @Test
        @DisplayName("status defaults to PENDING when not set")
        void statusDefaultsToPending() {
            Participant p = Participant.builder().trip(buildTrip()).userId(USER_ID).build();
            assertThat(p.getStatus()).isEqualTo(ParticipantStatus.PENDING);
        }

        @Test
        @DisplayName("joinedAt defaults to null before persist")
        void joinedAtIsNullByDefault() {
            Participant p = Participant.builder().trip(buildTrip()).userId(USER_ID).build();
            assertThat(p.getJoinedAt()).isNull();
        }
    }

    // =========================================================================
    // Builder with explicit values
    // =========================================================================

    @Nested
    @DisplayName("Builder with explicit values")
    class ExplicitValues {

        @Test
        @DisplayName("builds participant with all fields set correctly")
        void buildsFullParticipant() {
            Trip trip = buildTrip();
            Participant p = Participant.builder()
                    .trip(trip)
                    .userId(USER_ID)
                    .role(ParticipantRole.EDITOR)
                    .status(ParticipantStatus.ACCEPTED)
                    .build();

            assertThat(p.getTrip()).isSameAs(trip);
            assertThat(p.getUserId()).isEqualTo(USER_ID);
            assertThat(p.getRole()).isEqualTo(ParticipantRole.EDITOR);
            assertThat(p.getStatus()).isEqualTo(ParticipantStatus.ACCEPTED);
        }
    }

    // =========================================================================
    // JPA lifecycle callbacks
    // =========================================================================

    @Nested
    @DisplayName("JPA lifecycle callbacks")
    class LifecycleCallbacks {

        @Test
        @DisplayName("prePersist sets joinedAt when status is ACCEPTED")
        void prePersistSetsJoinedAtForAccepted() {
            Participant p = Participant.builder()
                    .trip(buildTrip())
                    .userId(USER_ID)
                    .status(ParticipantStatus.ACCEPTED)
                    .build();
            p.prePersist();
            assertThat(p.getJoinedAt()).isNotNull();
        }

        @Test
        @DisplayName("prePersist does not set joinedAt when status is PENDING")
        void prePersistDoesNotSetJoinedAtForPending() {
            Participant p = Participant.builder()
                    .trip(buildTrip())
                    .userId(USER_ID)
                    .status(ParticipantStatus.PENDING)
                    .build();
            p.prePersist();
            assertThat(p.getJoinedAt()).isNull();
        }

        @Test
        @DisplayName("prePersist does not set joinedAt when status is DECLINED")
        void prePersistDoesNotSetJoinedAtForDeclined() {
            Participant p = Participant.builder()
                    .trip(buildTrip())
                    .userId(USER_ID)
                    .status(ParticipantStatus.DECLINED)
                    .build();
            p.prePersist();
            assertThat(p.getJoinedAt()).isNull();
        }
    }
}
