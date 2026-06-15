package com.example.sportx.Controller;

import com.example.sportx.Entity.dto.SpotQueryDTO;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Entity.vo.SpotSearchResult;
import com.example.sportx.Search.SpotSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/spots/search")
@RequiredArgsConstructor
@Validated
@Tag(name = "Spot Search", description = "Elasticsearch-backed spot full-text search with IK analyzer and facets")
public class SpotSearchController {

    private final SpotSearchService spotSearchService;

    @PostMapping("/es")
    @Operation(summary = "Search spots via Elasticsearch",
            description = "IK full-text search on name/description/address with type/region/rating/open filters and facet aggregations")
    public Result<SpotSearchResult> searchByEs(@Valid @RequestBody SpotQueryDTO dto) {
        // ES 检索：关键词全文匹配 + 多维过滤 + 类型/地区聚合，与 MySQL 版 /spots/search 并存对比。
        return Result.success(spotSearchService.search(dto));
    }
}
