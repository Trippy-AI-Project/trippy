package pse.trippy.aiservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TravelAdviceRequest {

    @NotBlank
    @Size(max = 500)
    private String question;

    @NotBlank
    private String destination;

    @Size(max = 1000)
    private String context;
}
