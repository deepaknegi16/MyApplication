package com.hackerrank.hotel.dto;

/**
 * A hotel matched by the cross-city name search. Unlike
 * {@link HotelSearchResult} there is no distance — the search is not anchored
 * to a city center — so the city name is returned instead.
 */
public record HotelNameSearchResult(
        Long id,
        String name,
        double latitude,
        double longitude,
        int rating,
        String cityName) {
}
