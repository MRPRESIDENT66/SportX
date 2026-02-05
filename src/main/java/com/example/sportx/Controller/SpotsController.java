package com.example.sportx.Controller;

import cn.hutool.db.PageResult;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.sportx.Entity.Result;
import com.example.sportx.Entity.SpotQueryDTO;
import com.example.sportx.Entity.Spots;
import com.example.sportx.Service.ISpotsService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/spots")
public class SpotsController {

    @Resource
    public ISpotsService spotsService;

    @GetMapping("/{id}")
    public Result queryById(@PathVariable("id") long id) {
        return spotsService.queryById(id);
    }

    @PutMapping
    public Result updateSpots(@RequestBody Spots spots) {
        return spotsService.update(spots);
    }

    @PostMapping("/search")
    public Result<Spots> search(@RequestBody SpotQueryDTO dto) {
        return spotsService.querySpots(dto);
    }
}
