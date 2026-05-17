package ba.nwt.userservice.config;

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

    // ── Saga 4: User Deletion ─────────────────────────────────────────────────
    public static final String USER_DELETION_QUEUE           = "sportcenter.user.deletion.queue";
    public static final String USER_BOOKINGS_CANCELLED_QUEUE = "sportcenter.user.bookings.cancelled.queue";
    public static final String USER_BOOKINGS_FAILED_QUEUE    = "sportcenter.user.bookings.failed.queue";

    public static final String USER_DELETION_KEY           = "user.saga.deletion";
    public static final String USER_BOOKINGS_CANCELLED_KEY = "user.saga.bookings.cancelled";
    public static final String USER_BOOKINGS_FAILED_KEY    = "user.saga.bookings.failed";

    @Bean
    public TopicExchange sagaExchange() {
        return ExchangeBuilder.topicExchange(SAGA_EXCHANGE).durable(true).build();
    }

    @Bean public Queue userDeletionQueue()             { return QueueBuilder.durable(USER_DELETION_QUEUE).build(); }
    @Bean public Queue userBookingsCancelledQueue()    { return QueueBuilder.durable(USER_BOOKINGS_CANCELLED_QUEUE).build(); }
    @Bean public Queue userBookingsFailedQueue()       { return QueueBuilder.durable(USER_BOOKINGS_FAILED_QUEUE).build(); }

    @Bean public Binding userDeletionBinding()           { return BindingBuilder.bind(userDeletionQueue()).to(sagaExchange()).with(USER_DELETION_KEY); }
    @Bean public Binding userBookingsCancelledBinding()  { return BindingBuilder.bind(userBookingsCancelledQueue()).to(sagaExchange()).with(USER_BOOKINGS_CANCELLED_KEY); }
    @Bean public Binding userBookingsFailedBinding()     { return BindingBuilder.bind(userBookingsFailedQueue()).to(sagaExchange()).with(USER_BOOKINGS_FAILED_KEY); }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }
}
