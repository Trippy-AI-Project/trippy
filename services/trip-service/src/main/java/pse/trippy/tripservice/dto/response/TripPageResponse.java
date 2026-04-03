package pse.trippy.tripservice.dto.response;

import java.util.List;

public record TripPageResponse(
        List<TripResponse> trips,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
