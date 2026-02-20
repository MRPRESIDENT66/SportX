package com.example.sportx.Controller;

import com.example.sportx.Entity.Notification;
import com.example.sportx.Entity.User;
import com.example.sportx.Entity.vo.PageResult;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Service.NotificationQueryService;
import com.example.sportx.Utils.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
@Validated
@Tag(name = "Notification", description = "User notification APIs")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    @GetMapping("/list")
    @Operation(summary = "List notifications", description = "Get paged notifications for current user")
    public Result<PageResult<Notification>> listMyNotifications(
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") @Min(value = 1, message = "每页最少1条") @Max(value = 50, message = "每页最多50条") Integer size,
            @RequestParam(value = "isRead", required = false) Boolean isRead) {
        // 通知列表：支持按已读状态筛选。
        User user = UserHolder.getUser();
        return notificationQueryService.listMyNotifications(user == null ? null : user.getId(), page, size, isRead);
    }

    @PutMapping("/read/{id}")
    @Operation(summary = "Mark as read", description = "Mark one notification as read")
    public Result<Void> markAsRead(@PathVariable("id") @Positive(message = "通知ID必须大于0") Long id) {
        // 标记单条通知为已读（仅允许操作自己的通知）。
        User user = UserHolder.getUser();
        return notificationQueryService.markAsRead(user == null ? null : user.getId(), id);
    }
}
