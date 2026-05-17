package ba.nwt.bookingservice.saga;

import ba.nwt.bookingservice.config.RabbitMQConfig;
import ba.nwt.bookingservice.saga.event.BookingCancellationRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingCancellationSagaPublisher {

    private static final Logger log = LoggerFactory.getLogger(BookingCancellationSagaPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public void publishCancellationRequested(BookingCancellationRequestedEvent event) {
        log.info("[SAGA-CANCEL][{}] Publishing BookingCancellationRequestedEvent for bookingId={}",
                event.getSagaId(), event.getBookingId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.SAGA_EXCHANGE, RabbitMQConfig.BOOKING_CANCELLATION_KEY, event);
    }
}
