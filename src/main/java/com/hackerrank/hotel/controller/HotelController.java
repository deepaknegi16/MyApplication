package com.hackerrank.hotel.controller;

import com.hackerrank.hotel.dto.HotelSearchResult;
import com.hackerrank.hotel.model.Hotel;
import com.hackerrank.hotel.service.HotelService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HotelController {

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    // Q1: GET /hotel/{id}
    @GetMapping("/hotel/{id}")
    public ResponseEntity<Hotel> getHotelById(@PathVariable Long id) {
        return ResponseEntity.ok(hotelService.getHotelById(id));
    }

    // Q2: DELETE /hotel/{id} — soft delete only
    @DeleteMapping("/hotel/{id}")
    public ResponseEntity<Void> deleteHotelById(@PathVariable Long id) {
        hotelService.deleteHotelById(id);
        return ResponseEntity.noContent().build();
    }

    // Q3: GET /search/{cityId} — hotels sorted by distance to the city center
    @GetMapping("/search/{cityId}")
    public ResponseEntity<List<HotelSearchResult>> searchHotels(@PathVariable Long cityId) {
        return ResponseEntity.ok(hotelService.searchHotelsClosestToCityCenter(cityId));
    }
}
