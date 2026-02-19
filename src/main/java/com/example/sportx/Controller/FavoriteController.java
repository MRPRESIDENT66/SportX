package com.example.sportx.Controller;

import com.example.sportx.Entity.User;
import com.example.sportx.Entity.Spots;
import com.example.sportx.Entity.vo.PageResult;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Service.FavoriteService;
import com.example.sportx.Utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/spots/{id}")
    public Result<Void> addSpotFavorite(@PathVariable("id") Long spotId) {
        User user = UserHolder.getUser();
        return favoriteService.addSpotFavorite(user == null ? null : user.getId(), spotId);
    }

    @DeleteMapping("/spots/{id}")
    public Result<Void> deleteSpotFavorite(@PathVariable("id") Long spotId) {
        User user = UserHolder.getUser();
        return favoriteService.deleteSpotFavorite(user == null ? null : user.getId(), spotId);
    }

    @GetMapping("/spots")
    public Result<PageResult<Spots>> listSpotFavorites(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        User user = UserHolder.getUser();
        return favoriteService.listSpotFavorites(user == null ? null : user.getId(), page, size);
    }
}
