package com.example.sportx.Entity.vo;

import lombok.Data;

import java.util.List;

@Data
public class AiAskResponseVo {

    private String answer;
    private List<AiSourceVo> sources;
}
