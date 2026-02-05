package com.example.sportx.Service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.sportx.Entity.Result;
import com.example.sportx.Entity.SpotQueryDTO;
import com.example.sportx.Entity.PageResult;
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
    @Resource
    private SpotsMapper spotsMapper;

    @Override
    public Result queryById(long id) {

        // 解决缓存穿透
        // Spots spots = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY,id, Spots.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);
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

    @Override
    public Result querySpots(SpotQueryDTO spotQueryDTO) {

        int page = spotQueryDTO.getPage();
        int size = spotQueryDTO.getSize();

        Page<Spots> mpPage = new Page<>(page, size);
        LambdaQueryWrapper<Spots> qw = new LambdaQueryWrapper<>();

        // name 模糊查询
        if (spotQueryDTO.getName() != null && !spotQueryDTO.getName().isEmpty()) {
            qw.like(Spots::getName, spotQueryDTO.getName());
        }

        // 类型精确匹配
        if (spotQueryDTO.getType() != null && !spotQueryDTO.getType().isEmpty()) {
            qw.eq(Spots::getType, spotQueryDTO.getType());
        }

        // 地区
        if (spotQueryDTO.getRegion() != null && !spotQueryDTO.getRegion().isEmpty()) {
            qw.eq(Spots::getRegion, spotQueryDTO.getRegion());
        }

        // 最低评分
        if (spotQueryDTO.getMinRating() != null) {
            qw.ge(Spots::getRating, spotQueryDTO.getMinRating());
        }

        // 是否营业
        if (spotQueryDTO.getIsOpen() != null) {
            qw.eq(Spots::getIsOpen, spotQueryDTO.getIsOpen());
        }

        // 默认按评分倒序
        qw.orderByDesc(Spots::getRating);

        // 3. 执行分页查询
        Page<Spots> resultPage = spotsMapper.selectPage(mpPage, qw);

        // 4. 封装返回数据
        PageResult<Spots> result = new PageResult<>();
        result.setTotal(mpPage.getTotal());
        result.setPage(page);
        result.setSize(size);
        result.setRecords(resultPage.getRecords());

        return Result.success(result);
    }
}
