package com.hackerrank.hotel.controller;

import com.hackerrank.hotel.model.City;
import com.hackerrank.hotel.repository.CityRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class-level @RequestMapping: every method's path is relative to /city.
 * @CrossOrigin lets a browser SPA on another origin (here, a local dev
 * frontend) call this controller — in production, CORS is usually configured
 * globally in the security layer instead of per controller.
 */
@RestController
@RequestMapping("/city")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Cities", description = "Reference data for hotel search")
public class CityController {

    private final CityRepository cityRepository;

    public CityController(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    @Operation(summary = "List all cities")
    @GetMapping
    public List<City> getAllCities() {
        return cityRepository.findAll();
    }
}
