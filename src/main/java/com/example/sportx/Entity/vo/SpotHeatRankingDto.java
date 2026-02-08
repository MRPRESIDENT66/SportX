package com.example.sportx.Entity.vo;

import lombok.Data;

/**
 * 场馆热度榜返回 DTO。
 */
@Data
public class SpotHeatRankingDto {
    private int rank;
    private Long spotId;
    private String spotName;
    private Double score;
    private String region;
    private String type;
}

