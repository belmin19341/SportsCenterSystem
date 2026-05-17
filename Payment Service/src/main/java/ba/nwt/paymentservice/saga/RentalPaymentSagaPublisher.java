package ba.nwt.paymentservice.saga;

import ba.nwt.paymentservice.config.RabbitMQConfig;
import ba.nwt.paymentservice.saga.event.RentalPaymentCompletedEvent;
import ba.nwt.paymentservice.saga.event.RentalPaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RentalPaymentSagaPublisher {

    private static final Logger log = LoggerFactory.getLogger(RentalPaymentSagaPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public void publishCompleted(RentalPaymentCompletedEvent event) {
        log.info("[SAGA-RENTAL][{}] Publishing RentalPaymentCompletedEvent for rentalId={}", event.getSagaId(), event.getRentalId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.SAGA_EXCHANGE, RabbitMQConfig.RENTAL_PAYMENT_COMPLETED_KEY, event);
    }

    public void publishFailed(RentalPaymentFailedEvent event) {
        log.warn("[SAGA-RENTAL][{}] Publishing RentalPaymentFailedEvent for rentalId={}", event.getSagaId(), event.getRentalId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.SAGA_EXCHANGE, RabbitMQConfig.RENTAL_PAYMENT_FAILED_KEY, event);
    }
}
