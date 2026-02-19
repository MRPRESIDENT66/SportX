package com.example.sportx.Entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("spot_favorites")
public class SpotFavorite {
    private Long id;
    private String userId;
    private Long spotId;
    private LocalDateTime createTime;
}
