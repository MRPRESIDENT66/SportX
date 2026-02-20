package com.example.sportx.Controller;


import com.example.sportx.Entity.dto.LoginFormDto;
import com.example.sportx.Entity.dto.RegularLoginFormDto;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Entity.User;
import com.example.sportx.Entity.dto.UserProfileUpdateDto;
import com.example.sportx.Service.UserService;
import com.example.sportx.Utils.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Validated
@Tag(name = "User", description = "User authentication and profile APIs")
public class UserController {

    private final UserService userService;

    // 发送短信验证码（演示项目直接返回验证码，生产环境应走短信网关）。
    @PostMapping("code")
    @Operation(summary = "Send SMS code", description = "Generate and return an SMS code for phone login")
    public Result<String> sendCode(
            @RequestParam("phone")
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误") String phone,
            HttpSession session) {
        return userService.sendCode(phone,session);
    }

    @PostMapping("login")
    @Operation(summary = "Login by SMS code", description = "Validate phone + code and return access token")
    public Result<String> login(@Valid @RequestBody LoginFormDto loginFormDto, HttpSession session) {
        // 验证码登录：校验验证码后签发 token。
        return userService.login(loginFormDto,session);
    }

    @PostMapping("regularLogin")
    @Operation(summary = "Login by password", description = "Validate phone + password and return access token")
    public Result<String> regularLogin(@Valid @RequestBody RegularLoginFormDto regularLoginFormDto) {
        // 账号密码登录：密码使用 BCrypt 比对。
        return  userService.regularLogin(regularLoginFormDto);
    }

    @PostMapping("register")
    @Operation(summary = "Register", description = "Create a user account and return access token")
    public Result<String> register(@Valid @RequestBody RegularLoginFormDto regularLoginFormDto) {
        // 用户注册：创建账号并返回登录 token。
        return userService.register(regularLoginFormDto);
    }

    @PostMapping("logout")
    @Operation(summary = "Logout", description = "Invalidate current token in Redis")
    public Result<Void> logout(HttpServletRequest request) {
        // 注销：删除 Redis 登录态 token。
        String token = request.getHeader("authorization");
        return userService.logout(token);
    }


    @GetMapping("/me")
    @Operation(summary = "Current user", description = "Get current user from thread-local context")
    public Result<User> me() {
        // 返回当前线程上下文中的登录用户（由拦截器注入）。
        User user = UserHolder.getUser();
        return Result.success(user);
    }

    @GetMapping("/profile")
    @Operation(summary = "Get profile", description = "Get current user's profile")
    public Result<User> profile() {
        // 获取个人资料（包含头像、昵称、城市等）。
        User user = UserHolder.getUser();
        return userService.getProfile(user == null ? null : user.getId());
    }

    @PutMapping("/profile")
    @Operation(summary = "Update profile", description = "Update current user's profile fields")
    public Result<Void> updateProfile(@Valid @RequestBody UserProfileUpdateDto dto) {
        // 更新个人资料，字段级参数校验由 @Valid 触发。
        User user = UserHolder.getUser();
        return userService.updateProfile(user == null ? null : user.getId(), dto);
    }


}
