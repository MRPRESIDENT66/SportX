package com.example.sportx.Controller;

import com.example.sportx.Entity.Notification;
import com.example.sportx.Entity.User;
import com.example.sportx.Entity.vo.PageResult;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Service.NotificationQueryService;
import com.example.sportx.Utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    @GetMapping("/list")
    public Result<PageResult<Notification>> listMyNotifications(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "isRead", required = false) Boolean isRead) {
        User user = UserHolder.getUser();
        return notificationQueryService.listMyNotifications(user == null ? null : user.getId(), page, size, isRead);
    }

    @PutMapping("/read/{id}")
    public Result<Void> markAsRead(@PathVariable("id") Long id) {
        User user = UserHolder.getUser();
        return notificationQueryService.markAsRead(user == null ? null : user.getId(), id);
    }
}
