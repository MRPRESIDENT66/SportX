package com.example.sportx.Controller;

import com.example.sportx.Entity.FailedMessage;
import com.example.sportx.Entity.vo.PageResult;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Service.FailedMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mq/failed")
@RequiredArgsConstructor
public class FailedMessageController {

    private final FailedMessageService failedMessageService;

    @GetMapping("/list")
    public Result<PageResult<FailedMessage>> list(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "status", required = false) String status) {
        return failedMessageService.listFailedMessages(page, size, status);
    }

    @PostMapping("/retry/{id}")
    public Result<Void> retry(@PathVariable("id") Long id) {
        return failedMessageService.retryFailedMessage(id);
    }
}
