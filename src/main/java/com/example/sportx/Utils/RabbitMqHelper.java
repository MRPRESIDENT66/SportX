package com.example.sportx.Utils;

import com.example.sportx.Entity.ChallengeEvent;
import com.example.sportx.RabbitMQ.RabbitConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Helper for common RabbitMQ tasks like declaring queues/exchanges and sending JSON payloads.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMqHelper {

    private final RabbitTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;
    private final ObjectMapper objectMapper;

    /**
     * Ensure a durable queue exists (idempotent).
     */
    public void ensureQueue(String queueName) {
        Queue queue = QueueBuilder.durable(queueName).build();
        amqpAdmin.declareQueue(queue);
    }

    /**
     * Ensure a durable direct exchange exists (idempotent).
     */
    public void ensureDirectExchange(String exchangeName) {
        DirectExchange exchange = ExchangeBuilder.directExchange(exchangeName).durable(true).build();
        amqpAdmin.declareExchange(exchange);
    }

    /**
     * Ensure queue, exchange and binding exist before publishing.
     */
    public void ensureBinding(String queueName, String exchangeName, String routingKey) {
        ensureQueue(queueName);
        ensureDirectExchange(exchangeName);
        Binding binding = BindingBuilder.bind(new Queue(queueName)).to(new DirectExchange(exchangeName)).with(routingKey);
        amqpAdmin.declareBinding(binding);
    }

    /**
     * Publish a payload as JSON to a direct exchange.
     */
    public void sendJson(String exchange, String routingKey, Object payload) {
        sendJson(exchange, routingKey, payload, Collections.emptyMap());
    }

    /**
     * Publish a payload as JSON with custom headers.
     */
    public void sendJson(String exchange, String routingKey, Object payload, Map<String, Object> headers) {
        Objects.requireNonNull(routingKey, "routingKey must not be null");
        Message message = buildJsonMessage(payload, headers);
        if (message == null) {
            return;
        }
        rabbitTemplate.send(exchange, routingKey, message);
    }

    /**
     * Convenience for sending directly to a queue via the default exchange.
     */
    public void sendJsonToQueue(String queueName, Object payload) {
        ensureQueue(queueName);
        sendJson("", queueName, payload);
    }

    /**
     * Publish a challenge event, ensuring the infrastructure exists.
     */
    public void publishChallengeEvent(ChallengeEvent event) {
        if (event == null) {
            log.warn("Attempted to publish null challenge event");
            return;
        }
        ensureBinding(
                RabbitConstants.CHALLENGE_EVENT_QUEUE,
                RabbitConstants.CHALLENGE_EVENT_EXCHANGE,
                RabbitConstants.CHALLENGE_EVENT_ROUTING_KEY
        );
        sendJson(
                RabbitConstants.CHALLENGE_EVENT_EXCHANGE,
                RabbitConstants.CHALLENGE_EVENT_ROUTING_KEY,
                event
        );
    }

    private Message buildJsonMessage(Object payload, Map<String, Object> headers) {
        String body = serialize(payload);
        if (body == null) {
            return null;
        }
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        headers.forEach(properties::setHeader);
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }

    private String serialize(Object payload) {
        if (payload == null) {
            return null;
        }
        if (payload instanceof String) {
            return (String) payload;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            log.error("Failed to serialize payload for RabbitMQ", exception);
            return null;
        }
    }
}
