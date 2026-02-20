package com.example.sportx.Entity.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SpotQueryDTO {
    // page index, start from 1
    @Min(value = 1, message = "页码最小为1")
    private Integer page = 1;

    // page size
    @Min(value = 1, message = "每页最少1条")
    @Max(value = 50, message = "每页最多50条")
    private Integer size = 10;

    // query conditions (all optional)
    @Size(max = 100, message = "名称关键词最长100字符")
    private String name;      // fuzzy search
    @Size(max = 50, message = "类型最长50字符")
    private String type;      // exact type
    @Size(max = 50, message = "地区最长50字符")
    private String region;    // region / city
    @DecimalMin(value = "0.0", message = "最低评分不能小于0")
    @DecimalMax(value = "5.0", message = "最低评分不能大于5")
    private Double minRating; // minimum rating
    private Boolean isOpen;   // only open spots
}
