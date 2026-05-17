package ba.nwt.bookingservice.saga.event;

import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserBookingsCancellationFailedEvent {
    private String sagaId;
    private Long userId;
    private String reason;
    private LocalDateTime timestamp;
}
