package ba.nwt.bookingservice.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentCreateView {
    private Long userId;
    private Long bookingId;
    private Long rentalId;
    private BigDecimal amount;
    private BigDecimal depositAmount;
    private String paymentMethod;
    private String status;

    // Stripe / saved-card fields forwarded from the frontend
    private String stripeToken;
    private Long savedCardId;
    private Boolean saveCard;
    private String cardLast4;
    private String cardBrand;
}
