package com.example.sportx.Entity.dto;

import lombok.Data;

@Data
public class UserProfileUpdateDto {
    private String nickname;
    private String avatar;
    private String bio;
    private String gender;
    private String city;
}
