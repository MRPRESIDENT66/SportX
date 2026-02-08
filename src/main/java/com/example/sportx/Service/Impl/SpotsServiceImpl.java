package com.example.sportx.Service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.sportx.Entity.vo.PageResult;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Entity.dto.SpotQueryDTO;
import com.example.sportx.Entity.Spots;
import com.example.sportx.Mapper.SpotsMapper;
import com.example.sportx.Service.SpotsService;
import com.example.sportx.Utils.CacheClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.example.sportx.Utils.RedisConstants.*;

@Service
@RequiredArgsConstructor
public class SpotsServiceImpl extends ServiceImpl<SpotsMapper, Spots> implements SpotsService {

    private final StringRedisTemplate stringRedisTemplate;
    private final CacheClient cacheClient;
    private final SpotsMapper spotsMapper;

    @Override
    public Result<Spots> queryById(long id) {

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

    public Result<String> update(Spots spots) {
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
    public Result<PageResult<Spots>> querySpots(SpotQueryDTO spotQueryDTO) {

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
