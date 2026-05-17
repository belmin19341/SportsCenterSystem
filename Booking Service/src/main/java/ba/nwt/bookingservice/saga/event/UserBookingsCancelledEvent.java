package ba.nwt.bookingservice.saga.event;

import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserBookingsCancelledEvent {
    private String sagaId;
    private Long userId;
    private int cancelledCount;
    private LocalDateTime timestamp;
}
