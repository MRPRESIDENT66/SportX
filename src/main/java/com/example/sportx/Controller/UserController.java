package com.example.sportx.Controller;


import com.example.sportx.Entity.dto.LoginFormDto;
import com.example.sportx.Entity.dto.RegularLoginFormDto;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Entity.User;
import com.example.sportx.Entity.dto.UserProfileUpdateDto;
import com.example.sportx.Service.UserService;
import com.example.sportx.Utils.UserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    //发送短信验证码
    @PostMapping("code")
    public Result<String> sendCode(@RequestParam("phone") String phone, HttpSession session) {
        return userService.sendCode(phone,session);
    }

    @PostMapping("login")
    public Result<String> login(@RequestBody LoginFormDto loginFormDto, HttpSession session) {
        return userService.login(loginFormDto,session);
    }

    @PostMapping("regularLogin")
    public Result<String> regularLogin(@RequestBody RegularLoginFormDto regularLoginFormDto) {
        return  userService.regularLogin(regularLoginFormDto);
    }

    @PostMapping("register")
    public Result<String> register(@RequestBody RegularLoginFormDto regularLoginFormDto) {
        return userService.register(regularLoginFormDto);
    }

    @PostMapping("logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = request.getHeader("authorization");
        return userService.logout(token);
    }


    @GetMapping("/me")
    public Result<User> me() {
        User user = UserHolder.getUser();
        return Result.success(user);
    }

    @GetMapping("/profile")
    public Result<User> profile() {
        User user = UserHolder.getUser();
        return userService.getProfile(user == null ? null : user.getId());
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody UserProfileUpdateDto dto) {
        User user = UserHolder.getUser();
        return userService.updateProfile(user == null ? null : user.getId(), dto);
    }


}
