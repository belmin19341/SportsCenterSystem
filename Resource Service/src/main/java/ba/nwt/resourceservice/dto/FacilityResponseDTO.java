package ba.nwt.resourceservice.dto;

import ba.nwt.resourceservice.model.Facility;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FacilityResponseDTO {
    private Long id;
    private Long ownerId;
    private String name;
    private Facility.FacilityType type;
    private Integer capacity;
    private BigDecimal basePricePerHour;
    private String description;
    private String imageUrl;
    private LocalTime workingHoursStart;
    private LocalTime workingHoursEnd;
    private Facility.FacilityStatus status;
    private LocalDateTime createdAt;
}

