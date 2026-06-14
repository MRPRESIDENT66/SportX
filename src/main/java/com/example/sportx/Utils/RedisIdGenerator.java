package com.example.sportx.Utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Distributed, time-ordered ID generator backed by a per-day Redis counter.
 * <p>
 * Layout of the returned {@code long}:
 * <pre>
 *   | 31 bits seconds since EPOCH | 32 bits in-day sequence |
 * </pre>
 * IDs are globally unique across instances (the sequence lives in Redis),
 * monotonically increasing within a second, and partitioned per business tag
 * so unrelated streams never share a counter.
 */
@Component
public class RedisIdGenerator {

    /** Custom epoch: 2022-01-01T00:00:00Z (seconds). Keeps the timestamp part small. */
    private static final long EPOCH_SECONDS = 1640995200L;

    /** Bits reserved for the in-day sequence. */
    private static final int SEQUENCE_BITS = 32;

    /** Mask guarding against sequence overflow within a single day. */
    private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1;

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final String COUNTER_PREFIX = "id:seq:";

    private final StringRedisTemplate redisTemplate;

    public RedisIdGenerator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Generate the next unique id for the given business tag (e.g. {@code "participation"}).
     *
     * @param businessTag logical stream the id belongs to; also used as the Redis counter namespace
     * @return a 64-bit, time-ordered, globally unique id
     */
    public long nextId(String businessTag) {
        long timestamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - EPOCH_SECONDS;
        long sequence = nextSequence(businessTag) & SEQUENCE_MASK;
        return (timestamp << SEQUENCE_BITS) | sequence;
    }

    private long nextSequence(String businessTag) {
        // One counter key per tag per day so the sequence never grows unbounded
        // and stays comfortably inside SEQUENCE_BITS.
        String day = LocalDate.now().format(DAY_FORMAT);
        String counterKey = COUNTER_PREFIX + businessTag + ":" + day;
        Long sequence = redisTemplate.opsForValue().increment(counterKey);
        return sequence == null ? 0L : sequence;
    }
}
