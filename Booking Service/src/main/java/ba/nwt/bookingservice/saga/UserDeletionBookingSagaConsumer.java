package ba.nwt.bookingservice.saga;

import ba.nwt.bookingservice.config.RabbitMQConfig;
import ba.nwt.bookingservice.saga.event.UserDeletionRequestedEvent;
import ba.nwt.bookingservice.service.UserDeletionBookingSagaService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserDeletionBookingSagaConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserDeletionBookingSagaConsumer.class);
    private final UserDeletionBookingSagaService service;

    @RabbitListener(queues = RabbitMQConfig.USER_DELETION_QUEUE)
    public void onUserDeletionRequested(UserDeletionRequestedEvent event) {
        log.info("[SAGA-USER][{}] Received UserDeletionRequestedEvent for userId={}", event.getSagaId(), event.getUserId());
        service.cancelUserBookings(event);
    }
}
