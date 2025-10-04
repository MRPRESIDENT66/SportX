package com.example.sportx.Controller;

import com.example.sportx.RabbitMQ.HelloSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RabbitMQController {
    private final HelloSender sender;

    public RabbitMQController(HelloSender sender) {
        this.sender = sender;
    }

    // 在浏览器或 curl 里访问 http://localhost:8080/send?message=你的消息
    @GetMapping("/send")
    public String send(@RequestParam("message") String msg) {
        sender.send(msg);
        return "Sent: " + msg;
    }
}
