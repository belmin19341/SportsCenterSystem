package ba.nwt.userservice.saga;

import ba.nwt.userservice.config.RabbitMQConfig;
import ba.nwt.userservice.saga.event.UserBookingsCancelledEvent;
import ba.nwt.userservice.saga.event.UserBookingsCancellationFailedEvent;
import ba.nwt.userservice.service.UserDeletionSagaService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserDeletionSagaConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserDeletionSagaConsumer.class);
    private final UserDeletionSagaService service;

    @RabbitListener(queues = RabbitMQConfig.USER_BOOKINGS_CANCELLED_QUEUE)
    public void onUserBookingsCancelled(UserBookingsCancelledEvent event) {
        log.info("[SAGA-USER][{}] Received UserBookingsCancelledEvent for userId={}", event.getSagaId(), event.getUserId());
        service.finalizeDelete(event);
    }

    @RabbitListener(queues = RabbitMQConfig.USER_BOOKINGS_FAILED_QUEUE)
    public void onUserBookingsCancellationFailed(UserBookingsCancellationFailedEvent event) {
        log.warn("[SAGA-USER][{}] Received UserBookingsCancellationFailedEvent for userId={}", event.getSagaId(), event.getUserId());
        service.restoreUser(event);
    }
}
