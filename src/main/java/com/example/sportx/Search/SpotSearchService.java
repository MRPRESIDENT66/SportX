package com.example.sportx.Search;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import com.example.sportx.Entity.Spots;
import com.example.sportx.Entity.dto.SpotQueryDTO;
import com.example.sportx.Entity.vo.SpotSearchResult;
import com.example.sportx.Service.SpotsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 场馆 ES 检索服务：构建 bool 查询（IK 全文检索 + 多维过滤）并解析聚合 facet。
 *
 * <p>查询设计要点：
 * <ul>
 *   <li>关键词放 must 上下文走 multi_match，name 权重最高，参与相关性算分（_score 排序）；</li>
 *   <li>type/region/isOpen/minRating 放 filter 上下文：不算分、结果可被 ES 缓存，过滤更高效；</li>
 *   <li>同一次请求顺带返回 type/region 两个 terms 聚合，供前端渲染筛选侧边栏。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpotSearchService {

    private static final int MAX_SIZE = 50;
    private static final int FACET_SIZE = 20;
    private static final String TYPE_FACET = "type_facet";
    private static final String REGION_FACET = "region_facet";

    private final ElasticsearchOperations operations;
    private final SpotsService spotsService;
    private final SpotSearchRepository spotSearchRepository;

    /**
     * 确保 spots 索引按 @Field 注解的 IK 映射存在（不存在才建）。
     * 应用启动时调用，避免增量同步直接 save 触发 ES 动态映射（那样 name 字段不会用 IK 分词）。
     */
    public void ensureIndex() {
        IndexOperations indexOps = operations.indexOps(SpotDocument.class);
        if (!indexOps.exists()) {
            indexOps.createWithMapping();
            log.info("Created ES index 'spots' with IK mapping.");
        }
    }

    /**
     * 全量重建索引：删除旧索引 → 按映射重建 → 从 MySQL 批量灌入。
     * 用于初始化或映射变更后的重刷，是管理操作。
     */
    public int reindexAll() {
        IndexOperations indexOps = operations.indexOps(SpotDocument.class);
        if (indexOps.exists()) {
            indexOps.delete();
        }
        indexOps.createWithMapping();

        List<Spots> all = spotsService.list();
        if (all.isEmpty()) {
            return 0;
        }
        spotSearchRepository.saveAll(all.stream().map(SpotDocument::from).toList());
        log.info("Reindexed {} spots into ES.", all.size());
        return all.size();
    }

    public SpotSearchResult search(SpotQueryDTO dto) {
        int page = dto.getPage() == null || dto.getPage() < 1 ? 1 : dto.getPage();
        int size = dto.getSize() == null || dto.getSize() < 1 ? 10 : Math.min(dto.getSize(), MAX_SIZE);

        BoolQuery.Builder bool = new BoolQuery.Builder();

        // 关键词：多字段全文检索，name 权重最高；无关键词时匹配全部。
        if (StringUtils.hasText(dto.getName())) {
            String keyword = dto.getName().trim();
            bool.must(m -> m.multiMatch(mm -> mm
                    .query(keyword)
                    .fields("name^3", "description", "address")));
        } else {
            bool.must(m -> m.matchAll(ma -> ma));
        }

        // 过滤维度：filter 上下文不参与算分。
        if (StringUtils.hasText(dto.getType())) {
            bool.filter(f -> f.term(t -> t.field("type").value(dto.getType().trim())));
        }
        if (StringUtils.hasText(dto.getRegion())) {
            bool.filter(f -> f.term(t -> t.field("region").value(dto.getRegion().trim())));
        }
        if (dto.getIsOpen() != null) {
            bool.filter(f -> f.term(t -> t.field("isOpen").value(dto.getIsOpen())));
        }
        if (dto.getMinRating() != null) {
            double min = dto.getMinRating();
            bool.filter(f -> f.range(r -> r.number(n -> n.field("rating").gte(min))));
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(bool.build()))
                .withPageable(PageRequest.of(page - 1, size))
                .withAggregation(TYPE_FACET, Aggregation.of(a -> a.terms(t -> t.field("type").size(FACET_SIZE))))
                .withAggregation(REGION_FACET, Aggregation.of(a -> a.terms(t -> t.field("region").size(FACET_SIZE))))
                .build();

        SearchHits<SpotDocument> hits = operations.search(query, SpotDocument.class);

        List<SpotDocument> records = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        SpotSearchResult result = new SpotSearchResult();
        result.setTotal(hits.getTotalHits());
        result.setPage(page);
        result.setSize(size);
        result.setRecords(records);
        result.setTypeFacet(parseFacet(hits.getAggregations(), TYPE_FACET));
        result.setRegionFacet(parseFacet(hits.getAggregations(), REGION_FACET));
        return result;
    }

    /** 解析 terms 聚合结果为 {key -> docCount} 的有序 Map（按命中数降序，ES 默认顺序）。 */
    private Map<String, Long> parseFacet(AggregationsContainer<?> container, String name) {
        Map<String, Long> facet = new LinkedHashMap<>();
        if (!(container instanceof ElasticsearchAggregations esAggs)) {
            return facet;
        }
        ElasticsearchAggregation agg = esAggs.aggregationsAsMap().get(name);
        if (agg == null) {
            return facet;
        }
        agg.aggregation().getAggregate().sterms().buckets().array()
                .forEach(bucket -> facet.put(bucket.key().stringValue(), bucket.docCount()));
        return facet;
    }
}
