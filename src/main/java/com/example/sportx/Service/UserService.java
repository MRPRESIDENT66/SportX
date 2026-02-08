package com.example.sportx.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.sportx.Entity.dto.LoginFormDto;
import com.example.sportx.Entity.dto.RegularLoginFormDto;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Entity.User;
import com.example.sportx.Entity.dto.UserProfileUpdateDto;
import jakarta.servlet.http.HttpSession;


public interface UserService extends IService<User> {

    Result<String> sendCode(String phone, HttpSession session);

    Result<String> login(LoginFormDto loginFormDto, HttpSession session);

    Result<String> regularLogin(RegularLoginFormDto regularLoginFormDto);

    Result<String> register(RegularLoginFormDto regularLoginFormDto);

    Result<Void> logout(String token);

    Result<User> getProfile(String userId);

    Result<Void> updateProfile(String userId, UserProfileUpdateDto dto);
}
