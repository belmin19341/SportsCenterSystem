package ba.nwt.paymentservice.saga;

import ba.nwt.paymentservice.config.RabbitMQConfig;
import ba.nwt.paymentservice.saga.event.BookingCreatedEvent;
import ba.nwt.paymentservice.service.PaymentSagaService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentSagaConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentSagaConsumer.class);

    private final PaymentSagaService paymentSagaService;

    /**
     * Receives BookingCreatedEvent from Booking Service.
     * Executes local transaction 2: create and process the payment.
     */
    @RabbitListener(queues = RabbitMQConfig.BOOKING_CREATED_QUEUE)
    public void onBookingCreated(BookingCreatedEvent event) {
        log.info("[SAGA][{}] Received BookingCreatedEvent for bookingId={} amount={}",
                event.getSagaId(), event.getBookingId(), event.getTotalPrice());
        paymentSagaService.processPayment(event);
    }
}
