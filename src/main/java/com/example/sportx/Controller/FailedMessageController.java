package com.example.sportx.Controller;

import com.example.sportx.Entity.FailedMessage;
import com.example.sportx.Entity.vo.PageResult;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Service.FailedMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mq/failed")
@RequiredArgsConstructor
@Validated
@Tag(name = "MQ Failure", description = "Dead-letter inspection and replay APIs")
public class FailedMessageController {

    private final FailedMessageService failedMessageService;

    @GetMapping("/list")
    @Operation(summary = "List failed messages", description = "Get paged failed messages from DLQ persistence")
    public Result<PageResult<FailedMessage>> list(
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") @Min(value = 1, message = "每页最少1条") @Max(value = 50, message = "每页最多50条") Integer size,
            @RequestParam(value = "status", required = false) String status) {
        // 失败消息排查列表：支持按状态过滤（NEW/RETRIED/RETRY_FAILED）。
        return failedMessageService.listFailedMessages(page, size, status);
    }

    @PostMapping("/retry/{id}")
    @Operation(summary = "Replay failed message", description = "Replay one failed message by id")
    public Result<Void> retry(@PathVariable("id") @Positive(message = "失败消息ID必须大于0") Long id) {
        // 手动回放：将指定失败消息重新投递到主事件链路。
        return failedMessageService.retryFailedMessage(id);
    }
}
