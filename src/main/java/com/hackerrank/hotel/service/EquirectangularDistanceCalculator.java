package com.hackerrank.hotel.service;

import org.springframework.stereotype.Component;

/**
 * Cheaper flat-earth approximation — fine for ranking hotels inside one city,
 * noticeably wrong across continents. Exists to demonstrate multiple beans of
 * one type: inject it explicitly with
 * @Qualifier("equirectangularDistanceCalculator").
 */
@Component("equirectangularDistanceCalculator")
public class EquirectangularDistanceCalculator implements DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    @Override
    public double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double x = Math.toRadians(lon2 - lon1) * Math.cos(Math.toRadians((lat1 + lat2) / 2));
        double y = Math.toRadians(lat2 - lat1);
        return EARTH_RADIUS_KM * Math.sqrt(x * x + y * y);
    }
}
