package com.example.sportx.Entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    private Integer id;
    private String name;
    private String email;
    private Integer age;
    private LocalDateTime createdAt;

//    public User(Integer id, String name, String email, Integer age, LocalDateTime createdAt) {
//        this.id = id;
//        this.name = name;
//        this.email = email;
//        this.age = age;
//        this.createdAt = createdAt;
//    }
}
