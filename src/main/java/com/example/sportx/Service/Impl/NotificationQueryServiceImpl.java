package com.example.sportx.Service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.sportx.Entity.Notification;
import com.example.sportx.Entity.vo.PageResult;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Mapper.NotificationMapper;
import com.example.sportx.Service.NotificationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationQueryServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationQueryService {

    @Override
    public Result<PageResult<Notification>> listMyNotifications(String userId, Integer page, Integer size, Boolean isRead) {
        // 仅允许查询当前登录用户自己的通知。
        if (userId == null || userId.isBlank()) {
            return Result.error("用户未登录");
        }
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : Math.min(size, 50);

        Page<Notification> mpPage = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<Notification> qw = new LambdaQueryWrapper<>();
        qw.eq(Notification::getUserId, userId);
        if (isRead != null) {
            // isRead 为空时不过滤，支持“全部通知”查询。
            qw.eq(Notification::getIsRead, isRead);
        }
        qw.orderByDesc(Notification::getCreateTime);

        Page<Notification> resultPage = page(mpPage, qw);
        PageResult<Notification> result = new PageResult<>();
        result.setTotal(resultPage.getTotal());
        result.setPage(pageNo);
        result.setSize(pageSize);
        result.setRecords(resultPage.getRecords());
        return Result.success(result);
    }

    @Override
    public Result<Void> markAsRead(String userId, Long notificationId) {
        if (userId == null || userId.isBlank()) {
            return Result.error("用户未登录");
        }
        if (notificationId == null) {
            return Result.error("通知ID不能为空");
        }
        Notification notification = lambdaQuery()
                .eq(Notification::getId, notificationId)
                .eq(Notification::getUserId, userId)
                .one();
        // 同时校验 id + userId，防止越权修改他人通知状态。
        if (notification == null) {
            return Result.error("通知不存在或无权限");
        }
        if (Boolean.TRUE.equals(notification.getIsRead())) {
            return Result.success();
        }
        Notification toUpdate = new Notification();
        toUpdate.setId(notificationId);
        toUpdate.setIsRead(true);
        updateById(toUpdate);
        return Result.success();
    }
}
