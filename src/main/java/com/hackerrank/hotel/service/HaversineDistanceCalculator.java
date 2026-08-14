package com.hackerrank.hotel.service;

import com.hackerrank.hotel.util.HaversineUtil;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * The exact great-circle distance. @Primary makes this the default whenever
 * a DistanceCalculator is injected without a @Qualifier.
 */
@Component("haversineDistanceCalculator")
@Primary
public class HaversineDistanceCalculator implements DistanceCalculator {

    @Override
    public double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        return HaversineUtil.distanceKm(lat1, lon1, lat2, lon2);
    }
}
