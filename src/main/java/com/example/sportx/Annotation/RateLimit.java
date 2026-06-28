package com.example.sportx.Annotation;

import org.redisson.api.RateIntervalUnit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式接口限流注解，基于 Redisson 令牌桶实现。
 *
 * <p>贴在 Controller 方法上即可生效（由 {@code RateLimitAspect} 拦截），业务代码零侵入。
 * 约定：仅用于返回 {@code Result} 的方法——限流拒绝时切面直接返回 {@code Result.error}。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 一个时间窗口内允许的请求数。 */
    int rate();

    /** 时间窗口长度。 */
    int rateInterval();

    /** 时间窗口单位，默认秒。 */
    RateIntervalUnit unit() default RateIntervalUnit.SECONDS;

    /** true=每个用户独立限流(key 含 userId)；false=接口全局限流。 */
    boolean perUser() default true;
}
