package com.example.sportx.Entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Spots {
    private Long id; // 场所 ID

    private String name; // 场所名称，如「北京朝阳篮球馆」

    private String type; // 运动类型，如 "basketball", "gym", "yoga"

    private String description; // 场所描述或简介

    private String address; // 详细地址

    private String phone;  //联系电话

    private String region; // 地区，如 "beijing"

    private Integer visitCount; // 浏览次数（用于热度排行）

    private Double rating; // 平均评分（0~5）

    private Boolean isOpen; // 当前是否开放

    private String openTime;  // 营业时间

    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
}
