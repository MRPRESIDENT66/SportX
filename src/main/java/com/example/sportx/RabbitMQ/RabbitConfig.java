package com.example.sportx.RabbitMQ;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.sportx.RabbitMQ.RabbitConstants.CHALLENGE_EVENT_EXCHANGE;
import static com.example.sportx.RabbitMQ.RabbitConstants.CHALLENGE_EVENT_QUEUE;
import static com.example.sportx.RabbitMQ.RabbitConstants.CHALLENGE_EVENT_ROUTING_KEY;
import static com.example.sportx.RabbitMQ.RabbitConstants.CHALLENGE_EVENT_DLX_EXCHANGE;
import static com.example.sportx.RabbitMQ.RabbitConstants.CHALLENGE_EVENT_DLX_QUEUE;
import static com.example.sportx.RabbitMQ.RabbitConstants.CHALLENGE_EVENT_DLX_ROUTING_KEY;
import static com.example.sportx.RabbitMQ.RabbitConstants.SEARCH_SYNC_EXCHANGE;
import static com.example.sportx.RabbitMQ.RabbitConstants.SEARCH_SYNC_QUEUE;
import static com.example.sportx.RabbitMQ.RabbitConstants.SEARCH_SYNC_ROUTING_KEY;
import static com.example.sportx.RabbitMQ.RabbitConstants.SEARCH_SYNC_DLX_EXCHANGE;
import static com.example.sportx.RabbitMQ.RabbitConstants.SEARCH_SYNC_DLX_QUEUE;
import static com.example.sportx.RabbitMQ.RabbitConstants.SEARCH_SYNC_DLX_ROUTING_KEY;

@Configuration
public class RabbitConfig {

    /**
     * 全局 JSON 消息转换器：消费端 @Payload POJO 据此从 application/json 反序列化。
     * 注入 Spring 托管的 ObjectMapper（已注册 JavaTimeModule），支持 LocalDateTime。
     */
    @Bean
    public MessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

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

    // ── 搜索同步队列（含死信兜底）──────────────────────────────────────────────
    @Bean
    public Queue searchSyncQueue() {
        return QueueBuilder.durable(SEARCH_SYNC_QUEUE)
                .deadLetterExchange(SEARCH_SYNC_DLX_EXCHANGE)
                .deadLetterRoutingKey(SEARCH_SYNC_DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange searchSyncExchange() {
        return new DirectExchange(SEARCH_SYNC_EXCHANGE, true, false);
    }

    @Bean
    public Binding searchSyncBinding() {
        return BindingBuilder
                .bind(searchSyncQueue())
                .to(searchSyncExchange())
                .with(SEARCH_SYNC_ROUTING_KEY);
    }

    @Bean
    public Queue searchSyncDlxQueue() {
        return QueueBuilder.durable(SEARCH_SYNC_DLX_QUEUE).build();
    }

    @Bean
    public DirectExchange searchSyncDlxExchange() {
        return new DirectExchange(SEARCH_SYNC_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Binding searchSyncDlxBinding() {
        return BindingBuilder
                .bind(searchSyncDlxQueue())
                .to(searchSyncDlxExchange())
                .with(SEARCH_SYNC_DLX_ROUTING_KEY);
    }
}
