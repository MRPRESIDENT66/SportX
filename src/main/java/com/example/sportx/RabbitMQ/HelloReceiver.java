package com.example.sportx.RabbitMQ;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class HelloReceiver {
    @RabbitListener(queues = "hello-queue")
    public void receive(String message) {
        System.out.println(" [x] Received from queue: '" + message + "'");
    }
}
