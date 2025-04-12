package com.example.sportx.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.sportx.Entity.Result;
import com.example.sportx.Entity.Spots;


public interface ISpotsService extends IService<Spots> {
    Result queryById(long id);
}
