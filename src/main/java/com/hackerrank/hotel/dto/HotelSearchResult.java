package com.hackerrank.hotel.dto;

/**
 * A hotel matched by the city search, with its distance from the city center.
 */
public record HotelSearchResult(
        Long id,
        String name,
        double latitude,
        double longitude,
        int rating,
        double distanceFromCityCenterKm) {
}
