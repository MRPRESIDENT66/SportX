package com.example.sportx.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.sportx.Entity.LoginFormDto;
import com.example.sportx.Entity.RegularLoginFormDto;
import com.example.sportx.Entity.Result;
import com.example.sportx.Entity.User;
import jakarta.servlet.http.HttpSession;


public interface UserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDto loginFormDto, HttpSession session);

    Result regularLogin(RegularLoginFormDto  regularLoginFormDto);
}
