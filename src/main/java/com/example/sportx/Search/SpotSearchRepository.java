package com.example.sportx.Search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 场馆索引的 Spring Data ES 仓库。
 *
 * <p>提供开箱即用的 save / deleteById / findById 等文档级操作，供同步链路使用。
 * 复杂的多条件检索与聚合走 {@code ElasticsearchOperations}（见 SpotSearchService），
 * 不在此声明派生查询方法。
 */
@Repository
public interface SpotSearchRepository extends ElasticsearchRepository<SpotDocument, Long> {
}
