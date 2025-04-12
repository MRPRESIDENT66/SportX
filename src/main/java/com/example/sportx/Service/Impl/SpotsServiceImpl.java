package com.example.sportx.Service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.sportx.Entity.Result;
import com.example.sportx.Entity.Spots;
import com.example.sportx.Mapper.SpotsMapper;
import com.example.sportx.Service.ISpotsService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import static com.example.sportx.Utils.RedisConstants.CACHE_SHOP_KEY;

@Service
public class SpotsServiceImpl extends ServiceImpl<SpotsMapper, Spots> implements ISpotsService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(long id) {
        String key = CACHE_SHOP_KEY + id;

        String SpotsJson = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(SpotsJson)){
            Spots spots = BeanUtil.toBean(SpotsJson, Spots.class);
            return Result.success(spots);
        }
        Spots spots = getById(id);
        if(spots==null){
            return Result.error("该地点不存在！");
        }
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(spots));
        return Result.success(spots);
    }
}
