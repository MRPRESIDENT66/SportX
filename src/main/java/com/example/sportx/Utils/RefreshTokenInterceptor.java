package com.example.sportx.Utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.example.sportx.Entity.User;
import com.example.sportx.Entity.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {


        // 获取请求头中的token
        String token = request.getHeader("authorization");

        //基于token获取用户信息，没有查到信息
        if(StrUtil.isBlank(token)){
            return true;
        }
        // 提取用户的信息
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_TOKEN_KEY + token);
        if (userMap.isEmpty()) {
            return true;
        }
       //如果存在，保存用户信息到线程
        UserDto userDto = BeanUtil.fillBeanWithMap(userMap, new UserDto(), false);
        // 保存用户
        User user = BeanUtil.toBean(userDto, User.class);
        UserHolder.saveUser(user);

        stringRedisTemplate.expire(RedisConstants.LOGIN_TOKEN_KEY + token,RedisConstants.LOGIN_TOKEN_TTL, TimeUnit.MINUTES);
        // 放行
        return true;
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
