package com.example.sportx.RabbitMQ;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.sportx.RabbitMQ.RabbitConstants.CHALLENGE_EVENT_EXCHANGE;
import static com.example.sportx.RabbitMQ.RabbitConstants.CHALLENGE_EVENT_QUEUE;
import static com.example.sportx.RabbitMQ.RabbitConstants.CHALLENGE_EVENT_ROUTING_KEY;
import static com.example.sportx.RabbitMQ.RabbitConstants.CHALLENGE_EVENT_DLX_EXCHANGE;
import static com.example.sportx.RabbitMQ.RabbitConstants.CHALLENGE_EVENT_DLX_QUEUE;
import static com.example.sportx.RabbitMQ.RabbitConstants.CHALLENGE_EVENT_DLX_ROUTING_KEY;

@Configuration
public class RabbitConfig {

    @Bean
    public Queue challengeEventQueue() {
        return QueueBuilder.durable(CHALLENGE_EVENT_QUEUE)
                .deadLetterExchange(CHALLENGE_EVENT_DLX_EXCHANGE)
                .deadLetterRoutingKey(CHALLENGE_EVENT_DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange challengeEventExchange() {
        return new DirectExchange(CHALLENGE_EVENT_EXCHANGE, true, false);
    }

    @Bean
    public Binding challengeEventBinding() {
        return BindingBuilder
                .bind(challengeEventQueue())
                .to(challengeEventExchange())
                .with(CHALLENGE_EVENT_ROUTING_KEY);
    }

    @Bean
    public Queue challengeEventDlxQueue() {
        return QueueBuilder.durable(CHALLENGE_EVENT_DLX_QUEUE).build();
    }

    @Bean
    public DirectExchange challengeEventDlxExchange() {
        return new DirectExchange(CHALLENGE_EVENT_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Binding challengeEventDlxBinding() {
        return BindingBuilder
                .bind(challengeEventDlxQueue())
                .to(challengeEventDlxExchange())
                .with(CHALLENGE_EVENT_DLX_ROUTING_KEY);
    }
}
