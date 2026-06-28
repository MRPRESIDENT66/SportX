package com.example.sportx.Aspect;

import com.example.sportx.Annotation.RateLimit;
import com.example.sportx.Entity.User;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

/**
 * 接口限流切面：拦截 {@link RateLimit} 注解的方法，用 Redisson 令牌桶限流。
 *
 * <p>RateType.OVERALL：所有应用实例共享同一份配额，实现真正的分布式限流
 * （单机 Guava RateLimiter 做不到）。拿不到令牌即快速拒绝，不进入后续业务/事务。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private static final String KEY_PREFIX = "rate_limit:";

    private final RedissonClient redissonClient;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = buildKey(joinPoint, rateLimit);

        RRateLimiter limiter = redissonClient.getRateLimiter(key);
        // trySetRate 幂等：仅首次创建时设定速率，已存在则不覆盖。
        limiter.trySetRate(RateType.OVERALL, rateLimit.rate(), rateLimit.rateInterval(), rateLimit.unit());

        if (!limiter.tryAcquire(1)) {
            log.debug("Rate limit exceeded, key={}", key);
            return Result.error("请求过于频繁，请稍后再试");
        }
        return joinPoint.proceed();
    }

    /** 限流 key：全局=前缀+方法名；按用户=再拼当前登录用户 id。 */
    private String buildKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String method = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName();
        if (!rateLimit.perUser()) {
            return KEY_PREFIX + method;
        }
        User user = UserHolder.getUser();
        String userId = (user != null && user.getId() != null) ? user.getId() : "anonymous";
        return KEY_PREFIX + method + ":" + userId;
    }
}
