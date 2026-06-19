package com.example.sportx.Search;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import com.example.sportx.Entity.Challenge;
import com.example.sportx.Entity.dto.ChallengeListQueryDto;
import com.example.sportx.Entity.vo.ChallengeSearchResult;
import com.example.sportx.Service.ChallengeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * 挑战 ES 检索服务：构建 bool 查询（IK 全文检索 + 场馆/状态过滤）。
 *
 * <p>状态过滤基于 startTime/endTime 与当前日期的 range 推导，不存死状态字段，
 * 与 MySQL 版 listChallenges 逻辑一致，避免状态随时间过期。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeSearchService {

    private static final int MAX_SIZE = 50;

    private final ElasticsearchOperations operations;
    private final ChallengeService challengeService;
    private final ChallengeSearchRepository challengeSearchRepository;

    /** 确保 challenges 索引按 IK 映射存在（不存在才建）。 */
    public void ensureIndex() {
        IndexOperations indexOps = operations.indexOps(ChallengeDocument.class);
        if (!indexOps.exists()) {
            indexOps.createWithMapping();
            log.info("Created ES index 'challenges' with IK mapping.");
        }
    }

    /** 全量重建：删旧索引 → 按映射重建 → 从 MySQL 批量灌入。 */
    public int reindexAll() {
        IndexOperations indexOps = operations.indexOps(ChallengeDocument.class);
        if (indexOps.exists()) {
            indexOps.delete();
        }
        indexOps.createWithMapping();

        List<Challenge> all = challengeService.list();
        if (all.isEmpty()) {
            return 0;
        }
        challengeSearchRepository.saveAll(all.stream().map(ChallengeDocument::from).toList());
        log.info("Reindexed {} challenges into ES.", all.size());
        return all.size();
    }

    public ChallengeSearchResult search(ChallengeListQueryDto dto) {
        int page = dto.getPage() == null || dto.getPage() < 1 ? 1 : dto.getPage();
        int size = dto.getSize() == null || dto.getSize() < 1 ? 10 : Math.min(dto.getSize(), MAX_SIZE);

        BoolQuery.Builder bool = new BoolQuery.Builder();

        // 关键词：challengeName 权重最高，description 次之；无关键词时匹配全部。
        if (StringUtils.hasText(dto.getKeyword())) {
            String keyword = dto.getKeyword().trim();
            bool.must(m -> m.multiMatch(mm -> mm
                    .query(keyword)
                    .fields("challengeName^3", "description")));
        } else {
            bool.must(m -> m.matchAll(ma -> ma));
        }

        // 场馆过滤。
        if (dto.getSpotId() != null) {
            bool.filter(f -> f.term(t -> t.field("spotId").value(dto.getSpotId())));
        }

        // 状态过滤：按当前日期对 startTime/endTime 做 range 推导。
        String status = dto.getStatus();
        if (StringUtils.hasText(status)) {
            String today = LocalDate.now().toString();
            switch (status.trim().toLowerCase()) {
                case "upcoming" -> bool.filter(f -> f.range(r -> r.date(d -> d.field("startTime").gt(today))));
                case "ongoing" -> {
                    bool.filter(f -> f.range(r -> r.date(d -> d.field("startTime").lte(today))));
                    bool.filter(f -> f.range(r -> r.date(d -> d.field("endTime").gte(today))));
                }
                case "ended" -> bool.filter(f -> f.range(r -> r.date(d -> d.field("endTime").lt(today))));
                default -> { /* 非法状态忽略，不加过滤 */ }
            }
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(bool.build()))
                .withPageable(PageRequest.of(page - 1, size))
                .build();

        SearchHits<ChallengeDocument> hits = operations.search(query, ChallengeDocument.class);

        List<ChallengeDocument> records = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        ChallengeSearchResult result = new ChallengeSearchResult();
        result.setTotal(hits.getTotalHits());
        result.setPage(page);
        result.setSize(size);
        result.setRecords(records);
        return result;
    }
}
