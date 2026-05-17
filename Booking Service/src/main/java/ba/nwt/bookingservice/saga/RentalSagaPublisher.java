package ba.nwt.bookingservice.saga;

import ba.nwt.bookingservice.config.RabbitMQConfig;
import ba.nwt.bookingservice.saga.event.RentalCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RentalSagaPublisher {

    private static final Logger log = LoggerFactory.getLogger(RentalSagaPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public void publishRentalCreated(RentalCreatedEvent event) {
        log.info("[SAGA-RENTAL][{}] Publishing RentalCreatedEvent for rentalId={}", event.getSagaId(), event.getRentalId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.SAGA_EXCHANGE, RabbitMQConfig.RENTAL_CREATED_KEY, event);
    }
}
