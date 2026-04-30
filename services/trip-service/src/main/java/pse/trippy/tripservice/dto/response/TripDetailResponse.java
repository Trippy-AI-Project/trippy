package pse.trippy.tripservice.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TripDetailResponse(
        UUID id,
        String title,
        String destination,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        String visibility,
        int maxParticipants,
        String coverImageUrl,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        List<ParticipantResponse> participants
) {
}
