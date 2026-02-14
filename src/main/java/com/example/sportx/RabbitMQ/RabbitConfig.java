package com.example.sportx.RabbitMQ;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.sportx.RabbitMQ.RabbitConstants.CHALLENGE_EVENT_EXCHANGE;
import static com.example.sportx.RabbitMQ.RabbitConstants.CHALLENGE_EVENT_QUEUE;
import static com.example.sportx.RabbitMQ.RabbitConstants.CHALLENGE_EVENT_ROUTING_KEY;

@Configuration
public class RabbitConfig {

    @Bean
    public Queue challengeEventQueue() {
        return new Queue(CHALLENGE_EVENT_QUEUE, true);
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
}
