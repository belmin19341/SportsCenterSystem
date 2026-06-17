package ba.nwt.paymentservice.dto;

import ba.nwt.paymentservice.model.Payment;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentRequestDTO {

    private Long userId;
    private Long bookingId;
    private Long rentalId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @DecimalMin(value = "0.00", message = "Deposit amount cannot be negative")
    private BigDecimal depositAmount;

    @NotNull(message = "Payment method is required")
    private Payment.PaymentMethod paymentMethod;

    private Payment.PaymentStatus status;

    // Stripe payment fields (optional — omit for auto-approve fallback)
    private String stripeToken;   // tok_xxx from Stripe.js (one-time charge or new card)
    private Long savedCardId;     // ID of a previously saved SavedCard
    private Boolean saveCard;     // whether to persist card for future use
    private String cardLast4;     // sent by frontend alongside stripeToken
    private String cardBrand;     // sent by frontend alongside stripeToken
}

