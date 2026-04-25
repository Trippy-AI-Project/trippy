package pse.trippy.userservice.health;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQHealthIndicator implements HealthIndicator {
    private final ConnectionFactory connectionFactory;

    public RabbitMQHealthIndicator(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Health health() {
        try {
            connectionFactory.createConnection().close();
            return Health.up().withDetail("rabbitmq", "RabbitMQ is up").build();
        } catch (Exception ex) {
            return Health.down(ex).withDetail("rabbitmq", "RabbitMQ is down").build();
        }
    }
}package pse.trippy.userservice.health;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQHealthIndicator implements HealthIndicator {
    private final ConnectionFactory connectionFactory;

    public RabbitMQHealthIndicator(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Health health() {
        try {
            connectionFactory.createConnection().close();
            return Health.up().withDetail("rabbitmq", "RabbitMQ is up").build();
        } catch (Exception ex) {
            return Health.down(ex).withDetail("rabbitmq", "RabbitMQ is down").build();
        }
    }
}
