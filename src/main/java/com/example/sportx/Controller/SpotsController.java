package com.example.sportx.Controller;

import com.example.sportx.Entity.Result;
import com.example.sportx.Service.ISpotsService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/spots")
public class SpotsController {

    @Resource
    public ISpotsService spotsService;

    @GetMapping("/{id}")
    public Result queryById(@PathVariable("id") long id) {
        return spotsService.queryById(id);
    }
}
