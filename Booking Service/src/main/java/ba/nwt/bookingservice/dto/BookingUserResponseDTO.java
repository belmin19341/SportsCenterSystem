package ba.nwt.bookingservice.dto;

import ba.nwt.bookingservice.model.BookingUser;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingUserResponseDTO {
    private Long id;
    private Long bookingId;
    private Long userId;
    private BigDecimal amountDue;
    private BookingUser.PaymentStatus paymentStatus;
    private LocalDateTime invitedAt;
    private LocalDateTime paidAt;
}

