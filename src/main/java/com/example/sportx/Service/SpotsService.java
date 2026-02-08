package com.example.sportx.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Entity.dto.SpotQueryDTO;
import com.example.sportx.Entity.Spots;
import com.example.sportx.Entity.vo.PageResult;


public interface SpotsService extends IService<Spots> {
    Result<Spots> queryById(long id);

    Result<String> update(Spots spots);

    Result<PageResult<Spots>> querySpots(SpotQueryDTO spotQueryDTO);
}
