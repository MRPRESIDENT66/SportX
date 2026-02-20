package com.example.sportx.Entity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChallengeListQueryDto {
    @Min(value = 1, message = "页码最小为1")
    private Integer page = 1;
    @Min(value = 1, message = "每页最少1条")
    @Max(value = 50, message = "每页最多50条")
    private Integer size = 10;
    @Pattern(regexp = "^(upcoming|ongoing|ended)?$", message = "status仅支持 upcoming/ongoing/ended")
    private String status; // upcoming | ongoing | ended
    @Positive(message = "场馆ID必须大于0")
    private Long spotId;
    @Size(max = 100, message = "关键词最长100字符")
    private String keyword;
}
