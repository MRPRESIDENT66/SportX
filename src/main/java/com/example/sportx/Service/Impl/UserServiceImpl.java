package com.example.sportx.Service.Impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.sportx.Entity.User;
import com.example.sportx.Entity.dto.LoginFormDto;
import com.example.sportx.Entity.dto.RegularLoginFormDto;
import com.example.sportx.Entity.dto.UserDto;
import com.example.sportx.Entity.dto.UserProfileUpdateDto;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Mapper.UserMapper;
import com.example.sportx.Service.UserService;
import com.example.sportx.Utils.RegexUtils;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.example.sportx.Utils.RedisConstants.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final StringRedisTemplate stringRedisTemplate;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Result<String> sendCode(String phone, HttpSession session) {
        // 1) 基础校验：手机号格式不合法直接拒绝。
        if(!RegexUtils.isPhone(phone)){
            return Result.error("手机号格式错误！");
        }

        // 2) 生成 6 位随机验证码。
        String code = RandomUtil.randomNumbers(6);

        // 3) 写入 Redis 并设置过期时间，避免验证码长期有效。
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
        // 4) 演示阶段直接返回验证码，生产环境一般改为短信下发。
        return Result.success(code);
    }

    // 登录
    @Override
    public Result<String> login(LoginFormDto loginFormDto, HttpSession session) {
        String phone = loginFormDto.getPhone();
        String code = loginFormDto.getCode();
        if(!RegexUtils.isPhone(phone)){
            return Result.error("手机号格式错误！");
        }
        // 手机号维度登录风控：短时间失败过多则临时封禁。
        if (isLoginBlocked(phone)) {
            return Result.error("登录尝试过多，请30分钟后再试");
        }
        // 从 Redis 读取验证码进行校验。
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);

        if(cacheCode == null || !cacheCode.equals(code)){
            // 失败累计用于风控封禁。
            recordLoginFailure(phone);
            return Result.error("验证码错误！");
        }

        // 验证码登录可“免注册”：不存在则自动创建账号。
        User user = query().eq("phone",phone).one();
        if(user == null){
            user = createWithPhone(phone, null);
        }
        // 成功后清空失败计数，避免误封正常用户。
        clearLoginFailure(phone);
        return Result.success(persistLoginState(user));

    }

    public Result<String> regularLogin(RegularLoginFormDto regularLoginFormDto) {
        String phone = regularLoginFormDto.getPhone();
        String password = regularLoginFormDto.getPassword();
        if(!RegexUtils.isPhone(phone)){
            return Result.error("手机号格式错误！");
        }
        if (isLoginBlocked(phone)) {
            return Result.error("登录尝试过多，请30分钟后再试");
        }
        User user = query().eq("phone",phone).one();
        if(user == null){
            recordLoginFailure(phone);
            return Result.error("该用户不存在！");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            recordLoginFailure(phone);
            return Result.error("该账号未设置密码，请先使用验证码登录并设置密码");
        }
        if(passwordEncoder.matches(password, user.getPassword())){
            // 密码登录成功也清理风控计数。
            clearLoginFailure(phone);
            return Result.success(persistLoginState(user));
        }else{
            recordLoginFailure(phone);
            return Result.error("密码错误！");
        }
    }

    @Override
    public Result<String> register(RegularLoginFormDto regularLoginFormDto) {
        String phone = regularLoginFormDto.getPhone();
        String password = regularLoginFormDto.getPassword();
        if (!RegexUtils.isPhone(phone)) {
            return Result.error("手机号格式错误！");
        }
        if (password == null || password.trim().isEmpty()) {
            return Result.error("密码不能为空！");
        }
        User exists = query().eq("phone", phone).one();
        if (exists != null) {
            return Result.error("该手机号已注册！");
        }
        User user = createWithPhone(phone, passwordEncoder.encode(password));
        return Result.success(persistLoginState(user));
    }

    @Override
    public Result<Void> logout(String token) {
        if (token == null || token.isBlank()) {
            return Result.success();
        }
        stringRedisTemplate.delete(LOGIN_TOKEN_KEY + token);
        return Result.success();
    }

    @Override
    public Result<User> getProfile(String userId) {
        if (userId == null || userId.isBlank()) {
            return Result.error("用户未登录");
        }
        User user = getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @Override
    public Result<Void> updateProfile(String userId, UserProfileUpdateDto dto) {
        if (userId == null || userId.isBlank()) {
            return Result.error("用户未登录");
        }
        User toUpdate = new User();
        toUpdate.setId(userId);
        toUpdate.setNickname(dto.getNickname());
        toUpdate.setAvatar(dto.getAvatar());
        toUpdate.setBio(dto.getBio());
        toUpdate.setGender(dto.getGender());
        toUpdate.setCity(dto.getCity());
        updateById(toUpdate);
        return Result.success();
    }

    private User createWithPhone(String phone,String passwordHash) {
        // 当前项目用户ID使用随机数字生成，便于快速演示业务流程。
        User user = new User();
        user.setPhone(phone);
        user.setId(RandomUtil.randomNumbers(10));
        user.setPassword(passwordHash);
        save(user);
        return user;
    }

    /**
     * 将用户登录态写入Redis并返回token，供验证码登录和密码登录公用。
     */
    private String persistLoginState(User user) {
        // token 只存 Redis，服务端可随时失效会话，便于统一登录态管理。
        String token = UUID.randomUUID().toString(true);
        UserDto userDto = BeanUtil.copyProperties(user, UserDto.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDto, new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue == null ? null : fieldValue.toString()));
        stringRedisTemplate.opsForHash().putAll(LOGIN_TOKEN_KEY + token, userMap);
        stringRedisTemplate.expire(LOGIN_TOKEN_KEY + token, LOGIN_TOKEN_TTL, TimeUnit.MINUTES);
        return token;
    }

    private boolean isLoginBlocked(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        Boolean blocked = stringRedisTemplate.hasKey(LOGIN_BLOCK_KEY + phone);
        return Boolean.TRUE.equals(blocked);
    }

    private void recordLoginFailure(String phone) {
        if (phone == null || phone.isBlank()) {
            return;
        }
        // 失败次数在固定窗口累计，达到阈值后写入封禁键。
        String failKey = LOGIN_FAIL_KEY + phone;
        Long failedCount = stringRedisTemplate.opsForValue().increment(failKey);
        if (failedCount != null && failedCount == 1) {
            stringRedisTemplate.expire(failKey, LOGIN_FAIL_WINDOW_TTL, TimeUnit.MINUTES);
        }
        if (failedCount != null && failedCount >= LOGIN_FAIL_MAX) {
            stringRedisTemplate.opsForValue().set(LOGIN_BLOCK_KEY + phone, "1", LOGIN_BLOCK_TTL, TimeUnit.MINUTES);
        }
    }

    private void clearLoginFailure(String phone) {
        if (phone == null || phone.isBlank()) {
            return;
        }
        // 登录成功后清理计数和封禁键，恢复正常登录。
        stringRedisTemplate.delete(LOGIN_FAIL_KEY + phone);
        stringRedisTemplate.delete(LOGIN_BLOCK_KEY + phone);
    }
}
