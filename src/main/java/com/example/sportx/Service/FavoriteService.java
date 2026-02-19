package com.example.sportx.Service;

import com.example.sportx.Entity.Spots;
import com.example.sportx.Entity.vo.PageResult;
import com.example.sportx.Entity.vo.Result;

public interface FavoriteService {
    Result<Void> addSpotFavorite(String userId, Long spotId);
    Result<Void> deleteSpotFavorite(String userId, Long spotId);
    Result<PageResult<Spots>> listSpotFavorites(String userId, Integer page, Integer size);
}
