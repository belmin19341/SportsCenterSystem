package ba.nwt.paymentservice.dto;

import ba.nwt.paymentservice.model.Payment;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentRequestDTO {

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
}

