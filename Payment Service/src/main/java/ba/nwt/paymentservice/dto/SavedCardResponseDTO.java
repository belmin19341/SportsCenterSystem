package ba.nwt.paymentservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SavedCardResponseDTO {
    private Long id;
    private String last4;
    private String brand;
    private LocalDateTime createdAt;
}
