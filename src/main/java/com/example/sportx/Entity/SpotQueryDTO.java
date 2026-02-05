package com.example.sportx.Entity;

import lombok.Data;

@Data
public class SpotQueryDTO {
    // page index, start from 1
    private Integer page = 1;

    // page size
    private Integer size = 10;

    // query conditions (all optional)
    private String name;      // fuzzy search
    private String type;      // exact type
    private String region;    // region / city
    private Double minRating; // minimum rating
    private Boolean isOpen;   // only open spots
}
