package ba.nwt.bookingservice.saga;

import ba.nwt.bookingservice.config.RabbitMQConfig;
import ba.nwt.bookingservice.saga.event.BookingCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingSagaPublisher {

    private static final Logger log = LoggerFactory.getLogger(BookingSagaPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public void publishBookingCreated(BookingCreatedEvent event) {
        log.info("[SAGA][{}] Publishing BookingCreatedEvent for bookingId={}", event.getSagaId(), event.getBookingId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SAGA_EXCHANGE,
                RabbitMQConfig.BOOKING_CREATED_KEY,
                event
        );
    }
}
