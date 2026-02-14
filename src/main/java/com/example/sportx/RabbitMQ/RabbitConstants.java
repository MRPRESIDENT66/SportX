package com.example.sportx.RabbitMQ;

public final class RabbitConstants {

    private RabbitConstants() {
    }

    public static final String CHALLENGE_EVENT_QUEUE = "challenge-event-queue";
    public static final String CHALLENGE_EVENT_EXCHANGE = "challenge-event-exchange";
    public static final String CHALLENGE_EVENT_ROUTING_KEY = "challenge.event";
}
