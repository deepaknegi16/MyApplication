package com.hackerrank.hotel.service;

/**
 * Strategy interface: two beans implement this, so injection points must
 * disambiguate — either rely on the @Primary one or ask for a specific
 * bean with @Qualifier.
 */
public interface DistanceCalculator {

    double distanceKm(double lat1, double lon1, double lat2, double lon2);
}
