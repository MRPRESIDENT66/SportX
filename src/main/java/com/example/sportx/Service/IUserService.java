package com.example.sportx.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.sportx.Entity.LoginFormDto;
import com.example.sportx.Entity.Result;
import com.example.sportx.Entity.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;


public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDto loginFormDto, HttpSession session);
}
