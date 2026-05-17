package ba.nwt.bookingservice.saga;

import ba.nwt.bookingservice.config.RabbitMQConfig;
import ba.nwt.bookingservice.saga.event.RefundCompletedEvent;
import ba.nwt.bookingservice.saga.event.RefundFailedEvent;
import ba.nwt.bookingservice.service.BookingCancellationSagaService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingCancellationSagaConsumer {

    private static final Logger log = LoggerFactory.getLogger(BookingCancellationSagaConsumer.class);
    private final BookingCancellationSagaService service;

    /** Refund succeeded → finalize booking as CANCELLED (final state). */
    @RabbitListener(queues = RabbitMQConfig.REFUND_COMPLETED_QUEUE)
    public void onRefundCompleted(RefundCompletedEvent event) {
        log.info("[SAGA-CANCEL][{}] Received RefundCompletedEvent for bookingId={}", event.getSagaId(), event.getBookingId());
        service.finalizeCancel(event);
    }

    /** Refund failed → compensating action: restore booking to CONFIRMED. */
    @RabbitListener(queues = RabbitMQConfig.REFUND_FAILED_QUEUE)
    public void onRefundFailed(RefundFailedEvent event) {
        log.warn("[SAGA-CANCEL][{}] Received RefundFailedEvent for bookingId={} reason={}", event.getSagaId(), event.getBookingId(), event.getReason());
        service.restoreBooking(event);
    }
}
