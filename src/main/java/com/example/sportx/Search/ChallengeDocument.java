package com.example.sportx.Search;

import com.example.sportx.Entity.Challenge;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;

/**
 * 挑战在 Elasticsearch 中的索引模型，对应 {@code challenges} 索引。
 *
 * <p>映射策略与场馆一致：challengeName/description 用 IK 中文分词支持全文检索；
 * spotId 用 keyword 精确过滤；startTime/endTime 用 date 类型，查询时按当前日期做 range
 * 推导挑战状态（upcoming/ongoing/ended），与 MySQL 版逻辑保持一致——状态不存死，避免随时间过期。
 *
 * <p>{@code createIndex = false}：禁止 repository 启动时自动连 ES 建索引，
 * 索引由 {@link ChallengeSearchService#ensureIndex()} 与 reindex 显式管理，ES 不可用也不阻断启动。
 */
@Data
@NoArgsConstructor
@Document(indexName = "challenges", createIndex = false)
public class ChallengeDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String challengeName;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    @Field(type = FieldType.Long)
    private Long spotId;

    @Field(type = FieldType.Integer)
    private Integer totalSlots;

    @Field(type = FieldType.Integer)
    private Integer joinedSlots;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate startTime;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate endTime;

    /** 从 MySQL 实体转换为 ES 文档，供同步链路使用。 */
    public static ChallengeDocument from(Challenge challenge) {
        ChallengeDocument doc = new ChallengeDocument();
        doc.setId(challenge.getId());
        doc.setChallengeName(challenge.getChallengeName());
        doc.setDescription(challenge.getDescription());
        doc.setSpotId(challenge.getSpotId());
        doc.setTotalSlots(challenge.getTotalSlots());
        doc.setJoinedSlots(challenge.getJoinedSlots());
        doc.setStartTime(challenge.getStartTime());
        doc.setEndTime(challenge.getEndTime());
        return doc;
    }
}
