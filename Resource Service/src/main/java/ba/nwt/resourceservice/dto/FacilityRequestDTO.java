package ba.nwt.resourceservice.dto;

import ba.nwt.resourceservice.model.Facility;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FacilityRequestDTO {

    @NotNull(message = "Owner ID is required")
    private Long ownerId;

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must be at most 200 characters")
    private String name;

    @NotNull(message = "Facility type is required")
    private Facility.FacilityType type;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    @NotNull(message = "Base price per hour is required")
    @DecimalMin(value = "0.01", message = "Base price must be greater than 0")
    private BigDecimal basePricePerHour;

    private String description;

    @Size(max = 500)
    private String imageUrl;

    @NotNull(message = "Working hours start is required")
    private LocalTime workingHoursStart;

    @NotNull(message = "Working hours end is required")
    private LocalTime workingHoursEnd;

    private Facility.FacilityStatus status;
}

