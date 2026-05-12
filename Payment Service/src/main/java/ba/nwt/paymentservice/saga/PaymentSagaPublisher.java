package ba.nwt.paymentservice.saga;

import ba.nwt.paymentservice.config.RabbitMQConfig;
import ba.nwt.paymentservice.saga.event.PaymentCompletedEvent;
import ba.nwt.paymentservice.saga.event.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentSagaPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentSagaPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        log.info("[SAGA][{}] Publishing PaymentCompletedEvent for bookingId={} paymentId={}",
                event.getSagaId(), event.getBookingId(), event.getPaymentId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SAGA_EXCHANGE,
                RabbitMQConfig.PAYMENT_COMPLETED_KEY,
                event
        );
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        log.warn("[SAGA][{}] Publishing PaymentFailedEvent for bookingId={} reason={}",
                event.getSagaId(), event.getBookingId(), event.getReason());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SAGA_EXCHANGE,
                RabbitMQConfig.PAYMENT_FAILED_KEY,
                event
        );
    }
}
