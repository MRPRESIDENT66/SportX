package com.example.sportx.Controller;

import com.example.sportx.Entity.dto.ChallengeListQueryDto;
import com.example.sportx.Entity.vo.ChallengeSearchResult;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Search.ChallengeSearchService;
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
@RequestMapping("/challenge/search")
@RequiredArgsConstructor
@Validated
@Tag(name = "Challenge Search", description = "Elasticsearch-backed challenge full-text search with IK analyzer")
public class ChallengeSearchController {

    private final ChallengeSearchService challengeSearchService;

    @PostMapping("/es")
    @Operation(summary = "Search challenges via Elasticsearch",
            description = "IK full-text search on challengeName/description with spot and status filters")
    public Result<ChallengeSearchResult> searchByEs(@Valid @RequestBody ChallengeListQueryDto dto) {
        return Result.success(challengeSearchService.search(dto));
    }

    @PostMapping("/reindex")
    @Operation(summary = "Rebuild challenge index",
            description = "Drop and rebuild the challenges index from MySQL (admin/init operation)")
    public Result<Integer> reindex() {
        return Result.success(challengeSearchService.reindexAll());
    }
}
