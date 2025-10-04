package com.example.sportx.RabbitMQ;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class HelloSender {
    private final RabbitTemplate rabbitTemplate;

    public HelloSender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(String message) {
        // 发送到 hello-exchange，路由键为 "hello"
        rabbitTemplate.convertAndSend("hello-exchange", "hello", message);
        System.out.println(" [x] Sent via exchange: '" + message + "'");
    }
}
