package com.example.sportx.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.sportx.Entity.Notification;
import com.example.sportx.Entity.vo.PageResult;
import com.example.sportx.Entity.vo.Result;

public interface NotificationQueryService extends IService<Notification> {
    Result<PageResult<Notification>> listMyNotifications(String userId, Integer page, Integer size, Boolean isRead);

    Result<Void> markAsRead(String userId, Long notificationId);
}
