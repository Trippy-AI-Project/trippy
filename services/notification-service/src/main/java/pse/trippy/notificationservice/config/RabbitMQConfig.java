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
    public static final String ROUTING_INVITATION_ACCEPTED = "trip.invitation.accepted";
    public static final String ROUTING_TRIP_UPDATED = "trip.updated";
    public static final String ROUTING_PAYMENT_COMPLETED = "payment.completed";
    public static final String ROUTING_PAYMENT_FAILED = "payment.failed";

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
    public Binding invitationAcceptedBinding(Queue notificationQueue, TopicExchange trippyExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(trippyExchange)
                .with(ROUTING_INVITATION_ACCEPTED);
    }

    @Bean
    public Binding tripUpdatedBinding(Queue notificationQueue, TopicExchange trippyExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(trippyExchange)
                .with(ROUTING_TRIP_UPDATED);
    }

    @Bean
    public Binding paymentCompletedBinding(Queue notificationQueue, TopicExchange trippyExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(trippyExchange)
                .with(ROUTING_PAYMENT_COMPLETED);
    }

    @Bean
    public Binding paymentFailedBinding(Queue notificationQueue, TopicExchange trippyExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(trippyExchange)
                .with(ROUTING_PAYMENT_FAILED);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
