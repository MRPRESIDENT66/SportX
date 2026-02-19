package com.example.sportx.Entity.vo;

import lombok.Data;

@Data
public class UserScoreRankingDto {
    private Integer rank;
    private String userId;
    private String nickname;
    private String city;
    private Double score;
}
