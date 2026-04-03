package pse.trippy.notificationservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "trippy.events";
    public static final String QUEUE = "notification.events";
    public static final String DLQ = "notification.events.dlq";

    public static final String ROUTING_USER_REGISTERED = "user.registered";
    public static final String ROUTING_TRIP_INVITATION = "trip.invitation.created";

    @Bean
    public TopicExchange trippyExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", DLQ)
                .build();
    }

    @Bean
    public Binding userRegisteredBinding(Queue notificationQueue, TopicExchange trippyExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(trippyExchange)
                .with(ROUTING_USER_REGISTERED);
    }

    @Bean
    public Binding tripInvitationBinding(Queue notificationQueue, TopicExchange trippyExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(trippyExchange)
                .with(ROUTING_TRIP_INVITATION);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
