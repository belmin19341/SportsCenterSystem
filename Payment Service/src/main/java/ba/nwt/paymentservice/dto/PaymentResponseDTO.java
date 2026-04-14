package ba.nwt.paymentservice.dto;

import ba.nwt.paymentservice.model.Payment;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentResponseDTO {
    private Long id;
    private Long bookingId;
    private Long rentalId;
    private BigDecimal amount;
    private BigDecimal depositAmount;
    private Payment.PaymentMethod paymentMethod;
    private String transactionId;
    private Payment.PaymentStatus status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}

