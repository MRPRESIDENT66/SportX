package com.example.sportx.RabbitMQ;

public final class RabbitConstants {

    private RabbitConstants() {
    }

    public static final String CHALLENGE_EVENT_QUEUE = "challenge-event-queue";
    public static final String CHALLENGE_EVENT_EXCHANGE = "challenge-event-exchange";
    public static final String CHALLENGE_EVENT_ROUTING_KEY = "challenge.event";

    public static final String CHALLENGE_EVENT_DLX_EXCHANGE = "challenge-event-dlx-exchange";
    public static final String CHALLENGE_EVENT_DLX_QUEUE = "challenge-event-dlx-queue";
    public static final String CHALLENGE_EVENT_DLX_ROUTING_KEY = "challenge.event.dlx";
}
