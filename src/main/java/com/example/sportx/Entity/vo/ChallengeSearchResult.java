package com.example.sportx.Entity.vo;

import com.example.sportx.Search.ChallengeDocument;
import lombok.Data;

import java.util.List;

/**
 * 挑战 ES 检索结果：分页命中。
 *
 * <p>不带 facet——挑战缺少 type/region 这类天然分面维度，强行聚合意义不大，
 * 故按对象特点只做全文检索 + 状态/场馆过滤 + 相关性排序。
 */
@Data
public class ChallengeSearchResult {

    private long total;
    private int page;
    private int size;
    private List<ChallengeDocument> records;
}
