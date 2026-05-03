package ba.nwt.bookingservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceQuoteView {
    private Long facilityId;
    private BigDecimal totalPrice;
    private BigDecimal multiplier;
    private BigDecimal hours;
}
