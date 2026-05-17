package ba.nwt.bookingservice.saga;

import ba.nwt.bookingservice.config.RabbitMQConfig;
import ba.nwt.bookingservice.saga.event.UserBookingsCancelledEvent;
import ba.nwt.bookingservice.saga.event.UserBookingsCancellationFailedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserDeletionBookingSagaPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserDeletionBookingSagaPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public void publishBookingsCancelled(UserBookingsCancelledEvent event) {
        log.info("[SAGA-USER][{}] Publishing UserBookingsCancelledEvent for userId={} count={}", event.getSagaId(), event.getUserId(), event.getCancelledCount());
        rabbitTemplate.convertAndSend(RabbitMQConfig.SAGA_EXCHANGE, RabbitMQConfig.USER_BOOKINGS_CANCELLED_KEY, event);
    }

    public void publishBookingsCancellationFailed(UserBookingsCancellationFailedEvent event) {
        log.warn("[SAGA-USER][{}] Publishing UserBookingsCancellationFailedEvent for userId={}", event.getSagaId(), event.getUserId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.SAGA_EXCHANGE, RabbitMQConfig.USER_BOOKINGS_FAILED_KEY, event);
    }
}
