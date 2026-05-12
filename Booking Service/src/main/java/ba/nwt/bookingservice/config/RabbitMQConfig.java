package ba.nwt.bookingservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ── Exchange ──────────────────────────────────────────────────────────────
    public static final String SAGA_EXCHANGE = "sportcenter.saga.exchange";

    // ── Queue names ───────────────────────────────────────────────────────────
    /** Payment Service consumes this queue */
    public static final String BOOKING_CREATED_QUEUE   = "sportcenter.booking.created.queue";
    /** Booking Service consumes this queue */
    public static final String PAYMENT_COMPLETED_QUEUE = "sportcenter.payment.completed.queue";
    /** Booking Service consumes this queue */
    public static final String PAYMENT_FAILED_QUEUE    = "sportcenter.payment.failed.queue";

    // ── Routing keys ──────────────────────────────────────────────────────────
    public static final String BOOKING_CREATED_KEY   = "booking.saga.created";
    public static final String PAYMENT_COMPLETED_KEY = "payment.saga.completed";
    public static final String PAYMENT_FAILED_KEY    = "payment.saga.failed";

    // ── Exchange bean ─────────────────────────────────────────────────────────
    @Bean
    public TopicExchange sagaExchange() {
        return ExchangeBuilder.topicExchange(SAGA_EXCHANGE).durable(true).build();
    }

    // ── Queues declared by Booking Service (the ones it consumes) ─────────────
    @Bean
    public Queue paymentCompletedQueue() {
        return QueueBuilder.durable(PAYMENT_COMPLETED_QUEUE).build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(PAYMENT_FAILED_QUEUE).build();
    }

    // Also declare the queue that Payment Service will consume so RabbitMQ
    // creates it before the Payment Service starts (avoids AMQP 404 errors).
    @Bean
    public Queue bookingCreatedQueue() {
        return QueueBuilder.durable(BOOKING_CREATED_QUEUE).build();
    }

    // ── Bindings ──────────────────────────────────────────────────────────────
    @Bean
    public Binding bookingCreatedBinding() {
        return BindingBuilder.bind(bookingCreatedQueue())
                .to(sagaExchange()).with(BOOKING_CREATED_KEY);
    }

    @Bean
    public Binding paymentCompletedBinding() {
        return BindingBuilder.bind(paymentCompletedQueue())
                .to(sagaExchange()).with(PAYMENT_COMPLETED_KEY);
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(paymentFailedQueue())
                .to(sagaExchange()).with(PAYMENT_FAILED_KEY);
    }

    // ── JSON message converter ─────────────────────────────────────────────────
    // Inject Spring Boot's ObjectMapper (has JavaTimeModule → LocalDateTime works).
    // INFERRED type precedence: use @RabbitListener method's parameter type for
    // deserialization instead of the __TypeId__ header, which would contain the
    // sender's package name (not present in this service's classpath).
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }
}
