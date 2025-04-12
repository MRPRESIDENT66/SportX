package com.example.sportx.Service.Impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.sportx.Entity.LoginFormDto;
import com.example.sportx.Entity.Result;
import com.example.sportx.Entity.User;
import com.example.sportx.Entity.UserDto;
import com.example.sportx.Mapper.UserMapper;
import com.example.sportx.Service.IUserService;
import com.example.sportx.Utils.RegexUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.example.sportx.Utils.RedisConstants.*;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
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
    public Result login(LoginFormDto loginFormDto, HttpSession session) {
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
            creatWithPhone(phone,password);
        }
        // 随机生成token
        String token = UUID.randomUUID().toString(true);
        UserDto userDto= BeanUtil.copyProperties(user, UserDto.class);
        // 转化为map格式进行批量存储
        Map<String,Object> userMap = BeanUtil.beanToMap(userDto);

//        session.setAttribute("user", BeanUtil.copyProperties(user, UserDto.class));

        // 进行存储,并且设置30分钟后过期
        stringRedisTemplate.opsForHash().putAll(LOGIN_TOKEN_KEY+token,userMap);
        stringRedisTemplate.expire(LOGIN_TOKEN_KEY,LOGIN_TOKEN_TTL,TimeUnit.MINUTES);

        return Result.success(token);

    }

    private void creatWithPhone(String phone,String password) {
        User user = new User();
        user.setPhone(phone);
        user.setId("user_"+RandomUtil.randomNumbers(10));
        user.setPassword(password);
        save(user);
    }
}
