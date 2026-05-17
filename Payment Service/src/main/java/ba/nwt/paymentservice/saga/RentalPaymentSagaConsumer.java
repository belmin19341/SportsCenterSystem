package ba.nwt.paymentservice.saga;

import ba.nwt.paymentservice.config.RabbitMQConfig;
import ba.nwt.paymentservice.saga.event.RentalCreatedEvent;
import ba.nwt.paymentservice.service.RentalPaymentSagaService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RentalPaymentSagaConsumer {

    private static final Logger log = LoggerFactory.getLogger(RentalPaymentSagaConsumer.class);
    private final RentalPaymentSagaService service;

    @RabbitListener(queues = RabbitMQConfig.RENTAL_CREATED_QUEUE)
    public void onRentalCreated(RentalCreatedEvent event) {
        log.info("[SAGA-RENTAL][{}] Received RentalCreatedEvent for rentalId={}", event.getSagaId(), event.getRentalId());
        service.processRentalPayment(event);
    }
}
