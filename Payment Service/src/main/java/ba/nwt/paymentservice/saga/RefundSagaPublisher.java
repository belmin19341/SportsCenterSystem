package ba.nwt.paymentservice.saga;

import ba.nwt.paymentservice.config.RabbitMQConfig;
import ba.nwt.paymentservice.saga.event.RefundCompletedEvent;
import ba.nwt.paymentservice.saga.event.RefundFailedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefundSagaPublisher {

    private static final Logger log = LoggerFactory.getLogger(RefundSagaPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public void publishRefundCompleted(RefundCompletedEvent event) {
        log.info("[SAGA-CANCEL][{}] Publishing RefundCompletedEvent for bookingId={}", event.getSagaId(), event.getBookingId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.SAGA_EXCHANGE, RabbitMQConfig.REFUND_COMPLETED_KEY, event);
    }

    public void publishRefundFailed(RefundFailedEvent event) {
        log.warn("[SAGA-CANCEL][{}] Publishing RefundFailedEvent for bookingId={}", event.getSagaId(), event.getBookingId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.SAGA_EXCHANGE, RabbitMQConfig.REFUND_FAILED_KEY, event);
    }
}
