package com.example.sportx.Utils;

public final class NotificationKeys {

    private NotificationKeys() {
    }

    private static final String PREFIX = "notify:challenge";
    private static final String SCHEDULE_KEY = "notify:challenge:scheduled";

    public static String statusKey(long challengeId, String eventType, String userId) {
        return String.format("%s:%d:%s:%s", PREFIX, challengeId, eventType, userId);
    }

    public static String scheduledSetKey() {
        return SCHEDULE_KEY;
    }
}
