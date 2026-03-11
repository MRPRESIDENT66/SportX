package com.example.sportx.Entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Arrays;

@Data
public class AiAskRequestDto {

    @NotBlank(message = "question cannot be blank")
    @Size(max = 1000, message = "question must be at most 1000 characters")
    private String question;
}
