package ba.nwt.userservice.saga;

import ba.nwt.userservice.config.RabbitMQConfig;
import ba.nwt.userservice.saga.event.UserDeletionRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserDeletionSagaPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserDeletionSagaPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public void publishUserDeletionRequested(UserDeletionRequestedEvent event) {
        log.info("[SAGA-USER][{}] Publishing UserDeletionRequestedEvent for userId={}", event.getSagaId(), event.getUserId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.SAGA_EXCHANGE, RabbitMQConfig.USER_DELETION_KEY, event);
    }
}
