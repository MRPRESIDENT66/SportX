package com.example.sportx.Controller;

import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Entity.dto.SpotQueryDTO;
import com.example.sportx.Entity.Spots;
import com.example.sportx.Entity.vo.PageResult;
import com.example.sportx.Service.SpotsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/spots")
@RequiredArgsConstructor
@Validated
@Tag(name = "Spot", description = "Spot query and management APIs")
public class SpotsController {

    private final SpotsService spotsService;

    @GetMapping("/{id}")
    @Operation(summary = "Spot detail", description = "Get spot detail by id")
    public Result<Spots> queryById(@PathVariable("id") @Positive(message = "场馆ID必须大于0") long id) {
        // 场馆详情：服务层会优先走 Redis 缓存。
        return spotsService.queryById(id);
    }

    @PutMapping
    @Operation(summary = "Update spot", description = "Update spot data and invalidate cache")
    public Result<String> updateSpots(@RequestBody Spots spots) {
        // 更新场馆信息：写库后删除对应缓存，保证读写一致性。
        return spotsService.update(spots);
    }

    @PostMapping("/search")
    @Operation(summary = "Search spots", description = "Search spots with filters and pagination")
    public Result<PageResult<Spots>> search(@Valid @RequestBody SpotQueryDTO dto) {
        // 场馆检索：支持名称/类型/地区/评分/营业状态等组合条件分页查询。
        return spotsService.querySpots(dto);
    }
}
