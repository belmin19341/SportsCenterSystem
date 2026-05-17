package ba.nwt.paymentservice.saga;

import ba.nwt.paymentservice.config.RabbitMQConfig;
import ba.nwt.paymentservice.saga.event.BookingCancellationRequestedEvent;
import ba.nwt.paymentservice.service.RefundSagaService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefundSagaConsumer {

    private static final Logger log = LoggerFactory.getLogger(RefundSagaConsumer.class);
    private final RefundSagaService refundSagaService;

    @RabbitListener(queues = RabbitMQConfig.BOOKING_CANCELLATION_QUEUE)
    public void onBookingCancellationRequested(BookingCancellationRequestedEvent event) {
        log.info("[SAGA-CANCEL][{}] Received BookingCancellationRequestedEvent for bookingId={}", event.getSagaId(), event.getBookingId());
        refundSagaService.processRefund(event);
    }
}
