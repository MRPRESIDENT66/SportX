package com.example.sportx.Entity.vo;

import com.example.sportx.Search.SpotDocument;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 场馆 ES 检索结果：分页命中 + 聚合 facet。
 *
 * <p>facet 是 ES 相对 MySQL 的一大优势：一次查询同时返回结果列表和各维度的分组计数，
 * 前端可直接渲染"按类型/地区筛选"侧边栏（如 篮球(12) 游泳(8)），无需额外 count 查询。
 */
@Data
public class SpotSearchResult {

    private long total;
    private int page;
    private int size;
    private List<SpotDocument> records;

    /** 运动类型分布：type -> 命中数。 */
    private Map<String, Long> typeFacet;

    /** 地区分布：region -> 命中数。 */
    private Map<String, Long> regionFacet;
}
