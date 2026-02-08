package com.example.sportx.Entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {

    private String id;
    private String phone;
    private String password;
    private String nickname;
    private String avatar;
    private String bio;
    private String gender;
    private String city;

}
