package com.example.sportx.Service;

import com.example.sportx.Entity.dto.AiAskRequestDto;
import com.example.sportx.Entity.vo.AiAskResponseVo;
import com.example.sportx.Entity.vo.Result;

public interface AiService {

    Result<AiAskResponseVo> ask(AiAskRequestDto requestDto);
}
