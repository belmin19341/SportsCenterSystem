package ba.nwt.bookingservice.saga;

import ba.nwt.bookingservice.config.RabbitMQConfig;
import ba.nwt.bookingservice.saga.event.RentalPaymentCompletedEvent;
import ba.nwt.bookingservice.saga.event.RentalPaymentFailedEvent;
import ba.nwt.bookingservice.service.RentalSagaService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RentalSagaConsumer {

    private static final Logger log = LoggerFactory.getLogger(RentalSagaConsumer.class);
    private final RentalSagaService rentalSagaService;

    @RabbitListener(queues = RabbitMQConfig.RENTAL_PAYMENT_COMPLETED_QUEUE)
    public void onRentalPaymentCompleted(RentalPaymentCompletedEvent event) {
        log.info("[SAGA-RENTAL][{}] Received RentalPaymentCompletedEvent for rentalId={}", event.getSagaId(), event.getRentalId());
        rentalSagaService.confirmRental(event);
    }

    @RabbitListener(queues = RabbitMQConfig.RENTAL_PAYMENT_FAILED_QUEUE)
    public void onRentalPaymentFailed(RentalPaymentFailedEvent event) {
        log.warn("[SAGA-RENTAL][{}] Received RentalPaymentFailedEvent for rentalId={}", event.getSagaId(), event.getRentalId());
        rentalSagaService.cancelRental(event);
    }
}
