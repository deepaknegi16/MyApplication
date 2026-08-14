package com.hackerrank.hotel.service;

import com.hackerrank.hotel.dto.HotelSearchResult;
import com.hackerrank.hotel.model.Hotel;
import java.util.List;

public interface HotelService {

    /** Q1: fetch a hotel by id (404 if missing or soft-deleted). */
    Hotel getHotelById(Long id);

    /** Q2: soft-delete — mark the hotel as deleted, keep the row. */
    void deleteHotelById(Long id);

    /** Q3: all active hotels of a city, sorted by haversine distance to the city center. */
    List<HotelSearchResult> searchHotelsClosestToCityCenter(Long cityId);
}
