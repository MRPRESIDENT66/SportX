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
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.example.sportx.Utils.RedisConstants.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Result<String> sendCode(String phone, HttpSession session) {
        //1.校验手机号
        if(!RegexUtils.isPhone(phone)){
            //2.如果不符合返回错误信息
            return Result.error("手机号格式错误！");
        }

        //3.符合信息生成验证码
        String code = RandomUtil.randomNumbers(6);

        //4.保存验证码到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
//        session.setAttribute("code",code);
        //5.发送验证码
        return Result.success(code);
    }

    // 登录
    @Override
    public Result<String> login(LoginFormDto loginFormDto, HttpSession session) {
        String phone = loginFormDto.getPhone();
        String code = loginFormDto.getCode();
        String password = loginFormDto.getPassword();
        if(!RegexUtils.isPhone(phone)){
            return Result.error("手机号格式错误！");
        }
        // 从redis中取出数据
//        Object cacheCode = session.getAttribute("code");
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);

        if(cacheCode == null || !cacheCode.equals(code)){
            return Result.error("验证码错误！");
        }

        // 根据手机号查询用户是否存在
        User user = query().eq("phone",phone).one();
        if(user == null){
            // 用户如果不存在，保存用户
            user = createWithPhone(phone,password);
        }
        return Result.success(persistLoginState(user));

    }

    public Result<String> regularLogin(RegularLoginFormDto regularLoginFormDto) {
        String phone = regularLoginFormDto.getPhone();
        String password = regularLoginFormDto.getPassword();
        if(!RegexUtils.isPhone(phone)){
            return Result.error("手机号格式错误！");
        }
        User user = query().eq("phone",phone).one();
        if(user == null){
            return Result.error("该用户不存在！");
        }
        if(user.getPassword().equals(password)){
            return Result.success(persistLoginState(user));
        }else{
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
        User user = createWithPhone(phone, password);
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

    private User createWithPhone(String phone,String password) {
        User user = new User();
        user.setPhone(phone);
        user.setId(RandomUtil.randomNumbers(10));
        user.setPassword(password);
        save(user);
        return user;
    }

    /**
     * 将用户登录态写入Redis并返回token，供验证码登录和密码登录公用。
     */
    private String persistLoginState(User user) {
        String token = UUID.randomUUID().toString(true);
        UserDto userDto = BeanUtil.copyProperties(user, UserDto.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDto, new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue == null ? null : fieldValue.toString()));
        stringRedisTemplate.opsForHash().putAll(LOGIN_TOKEN_KEY + token, userMap);
        stringRedisTemplate.expire(LOGIN_TOKEN_KEY + token, LOGIN_TOKEN_TTL, TimeUnit.MINUTES);
        return token;
    }
}
