package com.hackerrank.hotel.service;

import com.hackerrank.hotel.dto.CreateHotelRequest;
import com.hackerrank.hotel.dto.HotelNameSearchResult;
import com.hackerrank.hotel.dto.HotelSearchResult;
import com.hackerrank.hotel.dto.UpdateHotelRequest;
import com.hackerrank.hotel.event.HotelDeletedEvent;
import com.hackerrank.hotel.exception.ResourceNotFoundException;
import com.hackerrank.hotel.model.City;
import com.hackerrank.hotel.model.Hotel;
import com.hackerrank.hotel.repository.CityRepository;
import com.hackerrank.hotel.repository.HotelRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final CityRepository cityRepository;
    private final DistanceCalculator distanceCalculator;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public HotelServiceImpl(HotelRepository hotelRepository,
                            CityRepository cityRepository,
                            @Qualifier("haversineDistanceCalculator") DistanceCalculator distanceCalculator,
                            ApplicationEventPublisher eventPublisher) {
        this.hotelRepository = hotelRepository;
        this.cityRepository = cityRepository;
        this.distanceCalculator = distanceCalculator;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "hotels", key = "#id")
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
    @CacheEvict(cacheNames = "hotels", key = "#id")
    public void deleteHotelById(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));
        hotel.setDeleted(true);
        hotelRepository.save(hotel);
        eventPublisher.publishEvent(new HotelDeletedEvent(
                hotel.getId(), hotel.getName(), currentUsername()));
    }

    @Override
    @Transactional
    public Hotel createHotel(CreateHotelRequest request) {
        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City not found with id: " + request.getCityId()));
        Hotel hotel = new Hotel(request.getName(), request.getLatitude(), request.getLongitude(), request.getRating(), city);
        hotel.setTime(request.getTime());
        return hotelRepository.save(hotel);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "hotels", key = "#id")
    public Hotel updateHotel(Long id, UpdateHotelRequest request) {
        Hotel hotel = hotelRepository.findById(id)
                .filter(h -> !h.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));
        hotel.setName(request.getName());
        hotel.setLatitude(request.getLatitude());
        hotel.setLongitude(request.getLongitude());
        hotel.setRating(request.getRating());
        hotel.setTime(request.getTime());
        // no explicit save needed: dirty checking flushes the UPDATE at commit
        return hotel;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelSearchResult> searchHotelsClosestToCityCenter(Long cityId, Integer limit) {
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new ResourceNotFoundException("City not found with id: " + cityId));

        var results = hotelRepository.findByCityIdAndDeletedFalse(cityId).stream()
                .map(hotel -> toSearchResult(hotel, city))
                .sorted(Comparator.comparingDouble(HotelSearchResult::distanceFromCityCenterKm));
        if (limit != null) {
            results = results.limit(limit);
        }
        return results.toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelNameSearchResult> searchHotelsByName(String name, Integer limit) {
        String query = name == null ? "" : name.trim();
        var results = hotelRepository
                .findByDeletedFalseAndNameContainingIgnoreCaseOrderByNameAsc(query).stream()
                .map(hotel -> new HotelNameSearchResult(
                        hotel.getId(),
                        hotel.getName(),
                        hotel.getLatitude(),
                        hotel.getLongitude(),
                        hotel.getRating(),
                        hotel.getCity() != null ? hotel.getCity().getName() : null));
        if (limit != null) {
            results = results.limit(limit);
        }
        return results.toList();
    }

    private HotelSearchResult toSearchResult(Hotel hotel, City city) {
        double distanceKm = distanceCalculator.distanceKm(
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

    private String currentUsername() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }
}
