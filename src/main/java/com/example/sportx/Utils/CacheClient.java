package com.example.sportx.Utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.sportx.Entity.RedisData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.example.sportx.Utils.RedisConstants.*;

@Slf4j
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, Object value, Long expire, TimeUnit timeUnit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), expire, timeUnit);
    }

    public void setWithLogicalExpire(String key, Object value, Long expire, TimeUnit timeUnit){
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(expire)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    public <R, ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long expire, TimeUnit timeUnit) {
        String key = keyPrefix + id;
        // 1) 先查缓存，命中直接返回。
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }

        // 2) 命中空字符串占位，说明该数据不存在（防穿透）。
        if(json != null){
            return null;
        }
        // 3) 回源数据库并回填缓存。
        R r = dbFallback.apply(id);
        if(r == null){
            this.set(key,"",CACHE_NULL_TTL, TimeUnit.SECONDS);
            return null;
        }
        this.set(key,r,expire,timeUnit);
        return r;
    }
    private static final ExecutorService CACHE_REBUILD_EXECUTE = Executors.newFixedThreadPool(10);

    public <R, ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long expire, TimeUnit timeUnit) {
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);

        // 1) 逻辑过期方案下，未预热则先回源并写入“带过期时间”的缓存对象。
        if (StrUtil.isBlank(json)) {
            R dbResult = dbFallback.apply(id);
            if (dbResult == null) {
                this.set(key, "", CACHE_NULL_TTL, TimeUnit.SECONDS);
                return null;
            }
            this.setWithLogicalExpire(key, dbResult, expire, timeUnit);
            return dbResult;
        }
        // 2) 反序列化缓存包装对象，读取业务数据 + 逻辑过期时间。
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        if (redisData == null || redisData.getData() == null || redisData.getExpireTime() == null) {
            return null;
        }
        R r =JSONUtil.toBean((JSONObject) redisData.getData(),type);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 3) 未过期直接返回旧值。
        if(expireTime.isAfter(LocalDateTime.now())){
            return r;
        }
        // 4) 已过期：尝试加锁异步重建，当前线程先返回旧值，降低请求抖动。
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        if(isLock){
            CACHE_REBUILD_EXECUTE.submit(()->{
                try{
                    //查询数据库
                    R r1 = dbFallback.apply(id);
                    if (r1 == null) {
                        // 空值也写占位，避免击穿后反复回源。
                        this.set(key, "", CACHE_NULL_TTL, TimeUnit.SECONDS);
                    } else {
                        // 写入新的逻辑过期缓存。
                        this.setWithLogicalExpire(key, r1, expire, timeUnit);
                    }
                }catch (Exception e){
                    throw new RuntimeException(e);
                }finally {
                    unlock(lockKey);
                }
            });
        }
        return r;

    }


    private boolean tryLock(String key){
        // 简单互斥锁：setIfAbsent + 短 TTL，避免重建任务堆积。
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }
}
