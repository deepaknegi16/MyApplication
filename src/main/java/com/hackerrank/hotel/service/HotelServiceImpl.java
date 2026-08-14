package com.hackerrank.hotel.service;

import com.hackerrank.hotel.dto.HotelSearchResult;
import com.hackerrank.hotel.exception.ResourceNotFoundException;
import com.hackerrank.hotel.model.City;
import com.hackerrank.hotel.model.Hotel;
import com.hackerrank.hotel.repository.CityRepository;
import com.hackerrank.hotel.repository.HotelRepository;
import com.hackerrank.hotel.util.HaversineUtil;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final CityRepository cityRepository;

    public HotelServiceImpl(HotelRepository hotelRepository, CityRepository cityRepository) {
        this.hotelRepository = hotelRepository;
        this.cityRepository = cityRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Hotel getHotelById(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));
        if (hotel.isDeleted()) {
            throw new ResourceNotFoundException("Hotel not found with id: " + id);
        }
        return hotel;
    }

    @Override
    @Transactional
    public void deleteHotelById(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));
        hotel.setDeleted(true);
        hotelRepository.save(hotel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelSearchResult> searchHotelsClosestToCityCenter(Long cityId) {
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new ResourceNotFoundException("City not found with id: " + cityId));

        return hotelRepository.findByCityIdAndDeletedFalse(cityId).stream()
                .map(hotel -> toSearchResult(hotel, city))
                .sorted(Comparator.comparingDouble(HotelSearchResult::distanceFromCityCenterKm))
                .toList();
    }

    private HotelSearchResult toSearchResult(Hotel hotel, City city) {
        double distanceKm = HaversineUtil.distanceKm(
                city.getLatitude(), city.getLongitude(),
                hotel.getLatitude(), hotel.getLongitude());
        return new HotelSearchResult(
                hotel.getId(),
                hotel.getName(),
                hotel.getLatitude(),
                hotel.getLongitude(),
                hotel.getRating(),
                Math.round(distanceKm * 100.0) / 100.0);
    }
}
