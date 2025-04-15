package com.example.sportx.Service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.sportx.Entity.Result;
import com.example.sportx.Entity.Spots;
import com.example.sportx.Mapper.SpotsMapper;
import com.example.sportx.Service.ISpotsService;
import com.example.sportx.Utils.CacheClient;
import jakarta.annotation.Resource;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.example.sportx.Utils.RedisConstants.*;

@Service
public class SpotsServiceImpl extends ServiceImpl<SpotsMapper, Spots> implements ISpotsService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(long id) {

        // 解决缓存穿透
//        Spots spots = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY,id, Spots.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);


        // 逻辑过期解决缓存击穿
        Spots spots = cacheClient
                .queryWithLogicalExpire(CACHE_SHOP_KEY,id,Spots.class, this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);

        // 判断空值
        if(spots == null){
            return Result.error("该地点不存在！");
        }

        return Result.success(spots);
    }

    public Result update(Spots spots) {
        Long id = spots.getId();
        if(id==null){
            return Result.error("店铺ID不能为空");
        }
        //更新数据库
        updateById(spots);
        //删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.success("更新成功！");
    }

}
