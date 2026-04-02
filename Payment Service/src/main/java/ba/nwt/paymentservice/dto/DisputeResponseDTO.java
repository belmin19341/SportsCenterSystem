package ba.nwt.paymentservice.dto;

import ba.nwt.paymentservice.model.Dispute;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DisputeResponseDTO {
    private Long id;
    private Long bookingId;
    private Long rentalId;
    private Long reporterId;
    private String description;
    private String evidenceUrl;
    private Dispute.DisputeStatus status;
    private String resolutionNote;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}

