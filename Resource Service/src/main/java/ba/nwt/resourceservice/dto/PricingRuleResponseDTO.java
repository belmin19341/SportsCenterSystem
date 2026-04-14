package ba.nwt.resourceservice.dto;

import ba.nwt.resourceservice.model.PricingRule;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PricingRuleResponseDTO {
    private Long id;
    private Long facilityId;
    private String facilityName;
    private LocalTime timeSlotStart;
    private LocalTime timeSlotEnd;
    private PricingRule.DayOfWeekEnum dayOfWeek;
    private BigDecimal priceMultiplier;
    private String description;
}

