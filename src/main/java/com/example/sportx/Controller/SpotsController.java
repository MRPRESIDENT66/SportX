package com.example.sportx.Controller;

import com.example.sportx.Entity.Result;
import com.example.sportx.Entity.SpotQueryDTO;
import com.example.sportx.Entity.Spots;
import com.example.sportx.Service.SpotsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/spots")
@RequiredArgsConstructor
public class SpotsController {

    private final SpotsService spotsService;

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
