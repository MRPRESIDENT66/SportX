package com.example.sportx.Search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 挑战索引的 Spring Data ES 仓库，提供文档级 save/deleteById/findById，供同步链路使用。
 * 复杂检索走 {@code ElasticsearchOperations}（见 ChallengeSearchService）。
 */
@Repository
public interface ChallengeSearchRepository extends ElasticsearchRepository<ChallengeDocument, Long> {
}
