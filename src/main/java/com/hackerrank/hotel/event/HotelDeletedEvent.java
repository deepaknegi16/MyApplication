package com.hackerrank.hotel.event;

/** Published by the service after a soft delete; consumed asynchronously. */
public record HotelDeletedEvent(Long hotelId, String hotelName, String deletedBy) {
}
