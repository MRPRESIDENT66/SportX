package com.example.sportx.Entity.dto;

import lombok.Data;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
public class UserProfileUpdateDto {
    @Size(max = 30, message = "昵称最多30个字符")
    private String nickname;
    @Size(max = 255, message = "头像链接长度不能超过255")
    private String avatar;
    @Size(max = 255, message = "个人简介最多255个字符")
    private String bio;
    @Pattern(regexp = "^(male|female|other)?$", message = "性别仅支持 male/female/other")
    private String gender;
    @Size(max = 50, message = "城市最多50个字符")
    private String city;
}
