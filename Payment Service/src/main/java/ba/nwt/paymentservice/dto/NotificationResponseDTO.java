package ba.nwt.paymentservice.dto;

import ba.nwt.paymentservice.model.Notification;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationResponseDTO {
    private Long id;
    private Long userId;
    private Notification.NotificationType type;
    private String subject;
    private String message;
    private LocalDateTime sentAt;
    private Boolean isRead;
}

