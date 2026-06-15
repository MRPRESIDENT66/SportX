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

    // ── 搜索同步链路：承载 spot/challenge 等聚合的 ES 索引同步事件 ──────────────
    public static final String SEARCH_SYNC_QUEUE = "search-sync-queue";
    public static final String SEARCH_SYNC_EXCHANGE = "search-sync-exchange";
    public static final String SEARCH_SYNC_ROUTING_KEY = "search.sync";

    public static final String SEARCH_SYNC_DLX_EXCHANGE = "search-sync-dlx-exchange";
    public static final String SEARCH_SYNC_DLX_QUEUE = "search-sync-dlx-queue";
    public static final String SEARCH_SYNC_DLX_ROUTING_KEY = "search.sync.dlx";
}
