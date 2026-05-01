package pse.trippy.aiservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryResponse {

    private String tripTitle;
    private String summary;
    private String totalEstimatedCost;
    private List<DayPlan> dailyPlan;
    private List<String> packingTips;
    private List<String> travelTips;
    private Instant generatedAt;
    private Integer tokensUsed;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayPlan {
        private Integer dayNumber;
        private String date;
        private String title;
        private List<Activity> activities;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Activity {
        private String time;
        private Integer duration;
        private String title;
        private String description;
        private String location;
        private String category; // SIGHTSEEING, FOOD, TRANSPORT, ACCOMMODATION, ACTIVITY, FREE_TIME
        private String estimatedCost;
        private String tips;
        private Boolean bookingRequired;
    }
}
