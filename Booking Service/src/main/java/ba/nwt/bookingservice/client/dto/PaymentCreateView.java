package ba.nwt.bookingservice.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentCreateView {
    private Long bookingId;
    private Long rentalId;
    private BigDecimal amount;
    private BigDecimal depositAmount;
    private String paymentMethod;
    private String status;
}
