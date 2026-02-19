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
        //从redis查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }

        // 判断命中的是否为空值
        if(json != null){
            return null;
        }
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

        // 逻辑过期方案在缓存未预热时，回退到DB并写入缓存，避免直接返回null
        if (StrUtil.isBlank(json)) {
            R dbResult = dbFallback.apply(id);
            if (dbResult == null) {
                this.set(key, "", CACHE_NULL_TTL, TimeUnit.SECONDS);
                return null;
            }
            this.setWithLogicalExpire(key, dbResult, expire, timeUnit);
            return dbResult;
        }
        //命中的话将信息转化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        if (redisData == null || redisData.getData() == null || redisData.getExpireTime() == null) {
            return null;
        }
        R r =JSONUtil.toBean((JSONObject) redisData.getData(),type);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())){
            // 未过期，返回商家信息
            return r;
        }
        // 已过期需要重建缓存
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        if(isLock){
            CACHE_REBUILD_EXECUTE.submit(()->{
                try{
                    //查询数据库
                    R r1 = dbFallback.apply(id);
                    if (r1 == null) {
                        this.set(key, "", CACHE_NULL_TTL, TimeUnit.SECONDS);
                    } else {
                        //写入redis
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
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }
}
