package com.example.sportx.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Envelope used by the logical-expiry caching strategy.
 * <p>
 * The payload is cached <em>without</em> a physical Redis TTL; instead {@link #expireAt}
 * marks the logical freshness deadline. Readers always get a value back (avoiding cache
 * breakdown) and trigger an asynchronous rebuild once the logical deadline has passed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogicalExpireWrapper {
    /** Logical freshness deadline; past this point the entry is rebuilt in the background. */
    private LocalDateTime expireAt;
    /** Cached business payload. */
    private Object payload;
}
