package pse.trippy.userservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for the User Service.
 *
 * <p>Declares the user events exchange and configures JSON message conversion.
 * Also subscribes to trip.events to receive trip.created notifications.
 */
@Configuration
public class RabbitMqConfig {

    public static final String USER_EVENTS_EXCHANGE = "user.events";

    /** Exchange published to by trip-service. */
    public static final String TRIP_EVENTS_EXCHANGE = "trip.events";

    /** Queue on which this service receives trip.created events. */
    public static final String TRIP_CREATED_QUEUE = "user-service.trip.created";

    @Bean
    public TopicExchange userEventsExchange() {
        return new TopicExchange(USER_EVENTS_EXCHANGE);
    }

    @Bean
    public TopicExchange tripEventsExchange() {
        return new TopicExchange(TRIP_EVENTS_EXCHANGE);
    }

    @Bean
    public Queue tripCreatedQueue() {
        return new Queue(TRIP_CREATED_QUEUE, true);
    }

    @Bean
    public Binding tripCreatedBinding(Queue tripCreatedQueue, TopicExchange tripEventsExchange) {
        return BindingBuilder.bind(tripCreatedQueue).to(tripEventsExchange).with("trip.created");
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
