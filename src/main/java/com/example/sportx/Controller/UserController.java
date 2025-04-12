package com.example.sportx.Controller;


import com.example.sportx.Entity.LoginFormDto;
import com.example.sportx.Entity.Result;
import com.example.sportx.Entity.User;
import com.example.sportx.Service.IUserService;
import com.example.sportx.Utils.UserHolder;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    //发送短信验证码
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {

        return userService.sendCode(phone,session);
    }

    @PostMapping("login")
    public Result login(@RequestBody LoginFormDto loginFormDto, HttpSession session) {

        return userService.login(loginFormDto,session);
    }

    @GetMapping("/me")
    public Result me() {
        User user = UserHolder.getUser();
        return Result.success(user);
    }
}
