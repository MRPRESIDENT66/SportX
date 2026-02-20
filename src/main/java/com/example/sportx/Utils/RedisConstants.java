package com.example.sportx.Utils;

public class RedisConstants {
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    public static final String LOGIN_TOKEN_KEY = "login:token:";
    public static final Long LOGIN_TOKEN_TTL = 30L;
    public static final String LOGIN_FAIL_KEY = "login:fail:";
    public static final String LOGIN_BLOCK_KEY = "login:block:";
    public static final Long LOGIN_FAIL_WINDOW_TTL = 10L;
    public static final Long LOGIN_BLOCK_TTL = 30L;
    public static final Long LOGIN_FAIL_MAX = 5L;
    public static final String CACHE_SHOP_KEY = "cache:shop:";
    public static final String CACHE_CHALLENGE_KEY = "cache:challenge:";
    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final String LOCK_CHALLENGE_JOIN_KEY = "lock:challenge:join:";
    public static final String CACHE_REBUILD_EXECUTE = "cache:rebuild:execute:";
    public static final Long CACHE_SHOP_TTL = 30L;    //正常缓存有效期
    public static final Long CACHE_CHALLENGE_TTL = 30L;
    public static final Long CACHE_NULL_TTL = 2L;     //空值缓存有效期

    // 排行榜相关 key
    public static final String LEADERBOARD_SPOT_HEAT_KEY = "leaderboard:spot:heat";
    public static final String LEADERBOARD_SPOT_RATING_KEY = "leaderboard:spot:rating";
    public static final String LEADERBOARD_CHALLENGE_HEAT_KEY = "leaderboard:challenge:heat";
    public static final String LEADERBOARD_USER_CHALLENGE_KEY = "leaderboard:user:challenge";
}
