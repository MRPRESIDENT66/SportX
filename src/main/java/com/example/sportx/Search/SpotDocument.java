package com.example.sportx.Search;

import com.example.sportx.Entity.Spots;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * 场馆在 Elasticsearch 中的索引模型，对应 {@code spots} 索引。
 *
 * <p>字段映射策略：
 * <ul>
 *   <li>中文文本字段（name/description/address）用 IK 分词：建索引用 {@code ik_max_word}
 *       做最细粒度切分提高召回，查询用 {@code ik_smart} 做粗粒度切分提高精度。
 *       这正是 MySQL {@code LIKE '%关键词%'} 做不到的——它不分词、不走索引。</li>
 *   <li>过滤维度（type/region/isOpen）用 {@code keyword} 精确匹配，配合 filter 上下文不算分、可缓存。</li>
 *   <li>数值字段（rating/visitCount）用对应数值类型，支持范围过滤与排序。</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@Document(indexName = "spots")
public class SpotDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String name;

    @Field(type = FieldType.Keyword)
    private String type;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String address;

    @Field(type = FieldType.Keyword)
    private String phone;

    @Field(type = FieldType.Keyword)
    private String region;

    @Field(type = FieldType.Integer)
    private Integer visitCount;

    @Field(type = FieldType.Double)
    private Double rating;

    @Field(type = FieldType.Boolean)
    private Boolean isOpen;

    @Field(type = FieldType.Keyword)
    private String openTime;

    /** 从 MySQL 实体转换为 ES 文档，供同步链路使用。 */
    public static SpotDocument from(Spots spot) {
        SpotDocument doc = new SpotDocument();
        doc.setId(spot.getId());
        doc.setName(spot.getName());
        doc.setType(spot.getType());
        doc.setDescription(spot.getDescription());
        doc.setAddress(spot.getAddress());
        doc.setPhone(spot.getPhone());
        doc.setRegion(spot.getRegion());
        doc.setVisitCount(spot.getVisitCount());
        doc.setRating(spot.getRating());
        doc.setIsOpen(spot.getIsOpen());
        doc.setOpenTime(spot.getOpenTime());
        return doc;
    }
}
