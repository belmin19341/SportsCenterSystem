package ba.nwt.bookingservice.saga;

import ba.nwt.bookingservice.config.RabbitMQConfig;
import ba.nwt.bookingservice.saga.event.PaymentCompletedEvent;
import ba.nwt.bookingservice.saga.event.PaymentFailedEvent;
import ba.nwt.bookingservice.service.BookingSagaService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingSagaConsumer {

    private static final Logger log = LoggerFactory.getLogger(BookingSagaConsumer.class);

    private final BookingSagaService bookingSagaService;

    /**
     * Happy path: payment succeeded → confirm the booking (final state).
     */
    @RabbitListener(queues = RabbitMQConfig.PAYMENT_COMPLETED_QUEUE)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("[SAGA][{}] Received PaymentCompletedEvent for bookingId={}, paymentId={}",
                event.getSagaId(), event.getBookingId(), event.getPaymentId());
        bookingSagaService.confirmBooking(event);
    }

    /**
     * Compensating transaction: payment failed → cancel the booking.
     * This is the inverse of the initial PENDING save (local transaction 1).
     */
    @RabbitListener(queues = RabbitMQConfig.PAYMENT_FAILED_QUEUE)
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.warn("[SAGA][{}] Received PaymentFailedEvent for bookingId={}, reason={}",
                event.getSagaId(), event.getBookingId(), event.getReason());
        bookingSagaService.cancelBooking(event);
    }
}
