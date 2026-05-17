package ba.nwt.bookingservice.saga.event;

import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserDeletionRequestedEvent {
    private String sagaId;
    private Long userId;
    private String username;
    private LocalDateTime timestamp;
}
