package com.hackerrank.hotel.service;

import com.hackerrank.hotel.dto.HotelNameSearchResult;
import com.hackerrank.hotel.dto.HotelSearchResult;
import com.hackerrank.hotel.dto.UpdateHotelRequest;
import com.hackerrank.hotel.model.Hotel;
import java.util.List;

public interface HotelService {

    /** Q1: fetch a hotel by id (404 if missing or soft-deleted). */
    Hotel getHotelById(Long id);

    /** Q2: soft-delete — mark the hotel as deleted, keep the row. */
    void deleteHotelById(Long id);

    /** Update a hotel's editable fields (404 if missing or soft-deleted). */
    Hotel updateHotel(Long id, UpdateHotelRequest request);

    /**
     * Q3: active hotels of a city, sorted by haversine distance to the city
     * center. A null limit returns all of them.
     */
    List<HotelSearchResult> searchHotelsClosestToCityCenter(Long cityId, Integer limit);

    /**
     * Active hotels across all cities whose name contains the given text
     * (case-insensitive), sorted by name. A null or blank name matches every
     * hotel; a null limit returns all matches.
     */
    List<HotelNameSearchResult> searchHotelsByName(String name, Integer limit);
}
