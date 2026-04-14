package ba.nwt.resourceservice.dto;

import ba.nwt.resourceservice.model.PricingRule;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PricingRuleRequestDTO {

    @NotNull(message = "Facility ID is required")
    private Long facilityId;

    @NotNull(message = "Time slot start is required")
    private LocalTime timeSlotStart;

    @NotNull(message = "Time slot end is required")
    private LocalTime timeSlotEnd;

    private PricingRule.DayOfWeekEnum dayOfWeek;

    @NotNull(message = "Price multiplier is required")
    @DecimalMin(value = "0.01", message = "Price multiplier must be greater than 0")
    private BigDecimal priceMultiplier;

    @Size(max = 255)
    private String description;
}

