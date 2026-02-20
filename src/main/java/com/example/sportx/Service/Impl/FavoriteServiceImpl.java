package com.example.sportx.Service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.sportx.Entity.SpotFavorite;
import com.example.sportx.Entity.Spots;
import com.example.sportx.Entity.vo.PageResult;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Mapper.SpotFavoriteMapper;
import com.example.sportx.Mapper.SpotsMapper;
import com.example.sportx.Service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final SpotFavoriteMapper spotFavoriteMapper;
    private final SpotsMapper spotsMapper;

    @Override
    public Result<Void> addSpotFavorite(String userId, Long spotId) {
        // 1) 登录与参数校验。
        if (userId == null || userId.isBlank()) {
            return Result.error("用户未登录");
        }
        if (spotId == null) {
            return Result.error("场馆ID不能为空");
        }

        // 2) 收藏前校验场馆存在，避免脏数据。
        Spots spot = spotsMapper.selectById(spotId);
        if (spot == null) {
            return Result.error("场馆不存在");
        }

        // 3) 应用层先查重，减少重复写库。
        LambdaQueryWrapper<SpotFavorite> qw = new LambdaQueryWrapper<>();
        qw.eq(SpotFavorite::getUserId, userId)
                .eq(SpotFavorite::getSpotId, spotId);
        Long count = spotFavoriteMapper.selectCount(qw);
        if (count != null && count > 0) {
            return Result.error("你已收藏该场馆");
        }

        SpotFavorite favorite = new SpotFavorite();
        favorite.setUserId(userId);
        favorite.setSpotId(spotId);
        try {
            spotFavoriteMapper.insert(favorite);
        } catch (DuplicateKeyException duplicateKeyException) {
            // 并发下唯一索引兜底命中，返回同样的业务语义。
            return Result.error("你已收藏该场馆");
        }
        return Result.success();
    }

    @Override
    public Result<Void> deleteSpotFavorite(String userId, Long spotId) {
        // 删除按 user_id + spot_id 条件执行，避免误删其他用户数据。
        if (userId == null || userId.isBlank()) {
            return Result.error("用户未登录");
        }
        if (spotId == null) {
            return Result.error("场馆ID不能为空");
        }

        Spots spot = spotsMapper.selectById(spotId);
        if (spot == null) {
            return Result.error("场馆不存在");
        }

        LambdaQueryWrapper<SpotFavorite> qw = new LambdaQueryWrapper<>();
        qw.eq(SpotFavorite::getUserId, userId)
                .eq(SpotFavorite::getSpotId, spotId);
        int deleted = spotFavoriteMapper.delete(qw);
        if (deleted < 1) {
            return Result.error("你并未收藏该场馆");
        }
        return Result.success();
    }

    @Override
    public Result<PageResult<Spots>> listSpotFavorites(String userId, Integer page, Integer size) {
        if (userId == null || userId.isBlank()) {
            return Result.error("用户未登录");
        }
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : Math.min(size, 50);

        Page<SpotFavorite> mpPage = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<SpotFavorite> qw = new LambdaQueryWrapper<>();
        qw.eq(SpotFavorite::getUserId, userId)
                .orderByDesc(SpotFavorite::getCreateTime);
        Page<SpotFavorite> favoritePage = spotFavoriteMapper.selectPage(mpPage, qw);

        List<SpotFavorite> favorites = favoritePage.getRecords();
        List<Spots> spotRecords;
        if (favorites == null || favorites.isEmpty()) {
            spotRecords = Collections.emptyList();
        } else {
            // 先批量查场馆，再按收藏顺序重排，保证返回顺序与收藏时间一致。
            List<Long> spotIds = favorites.stream().map(SpotFavorite::getSpotId).toList();
            List<Spots> spots = spotsMapper.selectBatchIds(spotIds);
            Map<Long, Spots> spotsById = spots.stream()
                    .collect(Collectors.toMap(Spots::getId, Function.identity(), (a, b) -> a));
            spotRecords = spotIds.stream()
                    .map(spotsById::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }

        PageResult<Spots> result = new PageResult<>();
        result.setTotal(favoritePage.getTotal());
        result.setPage(pageNo);
        result.setSize(pageSize);
        result.setRecords(spotRecords);
        return Result.success(result);
    }
}
