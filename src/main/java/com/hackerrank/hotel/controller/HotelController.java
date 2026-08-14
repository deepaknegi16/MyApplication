package com.hackerrank.hotel.controller;

import com.hackerrank.hotel.dto.HotelSearchResult;
import com.hackerrank.hotel.dto.UpdateHotelRequest;
import com.hackerrank.hotel.model.Hotel;
import com.hackerrank.hotel.service.HotelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Tag(name = "Hotels", description = "Retrieve, soft-delete and search hotels")
public class HotelController {

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @Operation(summary = "Get a hotel by id",
            description = "Soft-deleted hotels are treated as missing and return 404.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hotel found"),
            @ApiResponse(responseCode = "404", description = "Hotel missing or soft-deleted")
    })
    @GetMapping("/hotel/{id}")
    public ResponseEntity<Hotel> getHotelById(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(hotelService.getHotelById(id));
    }

    @Operation(summary = "Soft-delete a hotel",
            description = "Marks the hotel as deleted; the row is never removed. Requires the ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Hotel marked as deleted"),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
            @ApiResponse(responseCode = "404", description = "Hotel not found")
    })
    @DeleteMapping("/hotel/{id}")
    public ResponseEntity<Void> deleteHotelById(@PathVariable @Positive Long id) {
        hotelService.deleteHotelById(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search hotels closest to a city center",
            description = "Active hotels of the city sorted ascending by haversine distance from the city center.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sorted hotel list"),
            @ApiResponse(responseCode = "404", description = "City not found")
    })
    @GetMapping("/search/{cityId}")
    public ResponseEntity<List<HotelSearchResult>> searchHotels(
            @PathVariable @Positive Long cityId,
            @RequestParam(name = "limit", required = false) @Positive Integer limit) {
        return ResponseEntity.ok(hotelService.searchHotelsClosestToCityCenter(cityId, limit));
    }

    @Operation(summary = "Update a hotel",
            description = "Replaces the editable fields of an active hotel. Requires the ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated hotel"),
            @ApiResponse(responseCode = "400", description = "Invalid body"),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
            @ApiResponse(responseCode = "404", description = "Hotel missing or soft-deleted")
    })
    @PutMapping("/hotel/{id}")
    public ResponseEntity<Hotel> updateHotel(@PathVariable @Positive Long id,
                                             @Valid @RequestBody UpdateHotelRequest request) {
        return ResponseEntity.ok(hotelService.updateHotel(id, request));
    }
}
