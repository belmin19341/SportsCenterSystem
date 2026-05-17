package ba.nwt.paymentservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String SAGA_EXCHANGE = "sportcenter.saga.exchange";

    // ── Saga 1: Booking Creation ───────────────────────────────────────────────
    public static final String BOOKING_CREATED_QUEUE   = "sportcenter.booking.created.queue";
    public static final String PAYMENT_COMPLETED_QUEUE = "sportcenter.payment.completed.queue";
    public static final String PAYMENT_FAILED_QUEUE    = "sportcenter.payment.failed.queue";

    public static final String BOOKING_CREATED_KEY   = "booking.saga.created";
    public static final String PAYMENT_COMPLETED_KEY = "payment.saga.completed";
    public static final String PAYMENT_FAILED_KEY    = "payment.saga.failed";

    // ── Saga 2: Booking Cancellation + Refund ─────────────────────────────────
    public static final String BOOKING_CANCELLATION_QUEUE = "sportcenter.booking.cancellation.queue";
    public static final String REFUND_COMPLETED_QUEUE     = "sportcenter.refund.completed.queue";
    public static final String REFUND_FAILED_QUEUE        = "sportcenter.refund.failed.queue";

    public static final String BOOKING_CANCELLATION_KEY = "booking.saga.cancellation";
    public static final String REFUND_COMPLETED_KEY     = "refund.saga.completed";
    public static final String REFUND_FAILED_KEY        = "refund.saga.failed";

    // ── Saga 3: Rental + Payment ──────────────────────────────────────────────
    public static final String RENTAL_CREATED_QUEUE           = "sportcenter.rental.created.queue";
    public static final String RENTAL_PAYMENT_COMPLETED_QUEUE = "sportcenter.rental.payment.completed.queue";
    public static final String RENTAL_PAYMENT_FAILED_QUEUE    = "sportcenter.rental.payment.failed.queue";

    public static final String RENTAL_CREATED_KEY           = "rental.saga.created";
    public static final String RENTAL_PAYMENT_COMPLETED_KEY = "rental.payment.saga.completed";
    public static final String RENTAL_PAYMENT_FAILED_KEY    = "rental.payment.saga.failed";

    @Bean
    public TopicExchange sagaExchange() {
        return ExchangeBuilder.topicExchange(SAGA_EXCHANGE).durable(true).build();
    }

    @Bean public Queue bookingCreatedQueue()           { return QueueBuilder.durable(BOOKING_CREATED_QUEUE).build(); }
    @Bean public Queue paymentCompletedQueue()         { return QueueBuilder.durable(PAYMENT_COMPLETED_QUEUE).build(); }
    @Bean public Queue paymentFailedQueue()            { return QueueBuilder.durable(PAYMENT_FAILED_QUEUE).build(); }
    @Bean public Queue bookingCancellationQueue()      { return QueueBuilder.durable(BOOKING_CANCELLATION_QUEUE).build(); }
    @Bean public Queue refundCompletedQueue()          { return QueueBuilder.durable(REFUND_COMPLETED_QUEUE).build(); }
    @Bean public Queue refundFailedQueue()             { return QueueBuilder.durable(REFUND_FAILED_QUEUE).build(); }
    @Bean public Queue rentalCreatedQueue()            { return QueueBuilder.durable(RENTAL_CREATED_QUEUE).build(); }
    @Bean public Queue rentalPaymentCompletedQueue()   { return QueueBuilder.durable(RENTAL_PAYMENT_COMPLETED_QUEUE).build(); }
    @Bean public Queue rentalPaymentFailedQueue()      { return QueueBuilder.durable(RENTAL_PAYMENT_FAILED_QUEUE).build(); }

    @Bean public Binding bookingCreatedBinding()         { return BindingBuilder.bind(bookingCreatedQueue()).to(sagaExchange()).with(BOOKING_CREATED_KEY); }
    @Bean public Binding paymentCompletedBinding()       { return BindingBuilder.bind(paymentCompletedQueue()).to(sagaExchange()).with(PAYMENT_COMPLETED_KEY); }
    @Bean public Binding paymentFailedBinding()          { return BindingBuilder.bind(paymentFailedQueue()).to(sagaExchange()).with(PAYMENT_FAILED_KEY); }
    @Bean public Binding bookingCancellationBinding()    { return BindingBuilder.bind(bookingCancellationQueue()).to(sagaExchange()).with(BOOKING_CANCELLATION_KEY); }
    @Bean public Binding refundCompletedBinding()        { return BindingBuilder.bind(refundCompletedQueue()).to(sagaExchange()).with(REFUND_COMPLETED_KEY); }
    @Bean public Binding refundFailedBinding()           { return BindingBuilder.bind(refundFailedQueue()).to(sagaExchange()).with(REFUND_FAILED_KEY); }
    @Bean public Binding rentalCreatedBinding()          { return BindingBuilder.bind(rentalCreatedQueue()).to(sagaExchange()).with(RENTAL_CREATED_KEY); }
    @Bean public Binding rentalPaymentCompletedBinding() { return BindingBuilder.bind(rentalPaymentCompletedQueue()).to(sagaExchange()).with(RENTAL_PAYMENT_COMPLETED_KEY); }
    @Bean public Binding rentalPaymentFailedBinding()    { return BindingBuilder.bind(rentalPaymentFailedQueue()).to(sagaExchange()).with(RENTAL_PAYMENT_FAILED_KEY); }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }
}
