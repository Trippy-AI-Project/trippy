package pse.trippy.aiservice.dto.response;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelAdviceResponse {

    private String answer;
    private List<String> relatedQuestions;
    private Instant generatedAt;
}
