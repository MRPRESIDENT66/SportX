package com.example.sportx.Entity.dto;

import lombok.Data;

@Data
public class ChallengeListQueryDto {
    private Integer page = 1;
    private Integer size = 10;
    private String status; // upcoming | ongoing | ended
    private Long spotId;
    private String keyword;
}
