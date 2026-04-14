package ba.nwt.bookingservice.dto;

import ba.nwt.bookingservice.model.Booking;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingResponseDTO {
    private Long id;
    private Long userId;
    private Long facilityId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal totalPrice;
    private Boolean isRecurring;
    private String recurringPattern;
    private Booking.BookingStatus status;
    private LocalDateTime createdAt;
}

