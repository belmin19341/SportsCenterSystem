package ba.nwt.paymentservice.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DisputeRequestDTO {

    private Long bookingId;
    private Long rentalId;

    @NotNull(message = "Reporter ID is required")
    private Long reporterId;

    @NotBlank(message = "Description is required")
    private String description;

    @Size(max = 500, message = "Evidence URL must be at most 500 characters")
    private String evidenceUrl;
}

