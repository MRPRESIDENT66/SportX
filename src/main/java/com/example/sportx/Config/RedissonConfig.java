package com.example.sportx.Config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 客户端配置。
 *
 * <p>只引入 redisson 核心包并手动声明客户端，与现有 Spring Data Redis(Lettuce) 各自独立，
 * 互不干扰：Lettuce 继续承担缓存/计数等常规操作，Redisson 专门提供分布式锁等高级原语。
 * 连接复用同一套 Redis 地址配置。
 */
@Configuration
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(
            @Value("${spring.data.redis.host:127.0.0.1}") String host,
            @Value("${spring.data.redis.port:6379}") int port) {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + host + ":" + port);
        return Redisson.create(config);
    }
}
