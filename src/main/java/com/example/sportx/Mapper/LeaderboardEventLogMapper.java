package com.example.sportx.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.sportx.Entity.LeaderboardEventLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface LeaderboardEventLogMapper extends BaseMapper<LeaderboardEventLog> {

    /** 查询某用户当前在 DB 中的积分总和，用于与 Redis ZSet score 比对。 */
    @Select("SELECT COALESCE(SUM(user_delta), 0) FROM leaderboard_event_log WHERE user_id = #{userId}")
    double sumUserScore(@Param("userId") String userId);

    /** 查询某场馆当前在 DB 中的热度总和，用于与 Redis ZSet score 比对。 */
    @Select("SELECT COALESCE(SUM(spot_delta), 0) FROM leaderboard_event_log WHERE spot_id = #{spotId}")
    double sumSpotHeat(@Param("spotId") Long spotId);
}
