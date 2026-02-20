package com.example.sportx.Controller;

import com.example.sportx.Entity.User;
import com.example.sportx.Entity.Spots;
import com.example.sportx.Entity.vo.PageResult;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Service.FavoriteService;
import com.example.sportx.Utils.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/favorite")
@RequiredArgsConstructor
@Validated
@Tag(name = "Favorite", description = "Spot favorite APIs")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/spots/{id}")
    @Operation(summary = "Add favorite", description = "Favorite one spot for current user")
    public Result<Void> addSpotFavorite(@PathVariable("id") @Positive(message = "场馆ID必须大于0") Long spotId) {
        // 收藏场馆：user_id + spot_id 唯一约束防重复。
        User user = UserHolder.getUser();
        return favoriteService.addSpotFavorite(user == null ? null : user.getId(), spotId);
    }

    @DeleteMapping("/spots/{id}")
    @Operation(summary = "Remove favorite", description = "Cancel favorite for one spot")
    public Result<Void> deleteSpotFavorite(@PathVariable("id") @Positive(message = "场馆ID必须大于0") Long spotId) {
        // 取消收藏：按当前用户 + 场馆ID 删除收藏关系。
        User user = UserHolder.getUser();
        return favoriteService.deleteSpotFavorite(user == null ? null : user.getId(), spotId);
    }

    @GetMapping("/spots")
    @Operation(summary = "List favorites", description = "Get paged favorite spot list for current user")
    public Result<PageResult<Spots>> listSpotFavorites(
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") @Min(value = 1, message = "每页最少1条") @Max(value = 50, message = "每页最多50条") Integer size) {
        // 我的收藏列表：按收藏时间倒序分页返回场馆详情。
        User user = UserHolder.getUser();
        return favoriteService.listSpotFavorites(user == null ? null : user.getId(), page, size);
    }
}
