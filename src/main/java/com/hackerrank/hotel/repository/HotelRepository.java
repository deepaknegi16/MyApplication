package com.hackerrank.hotel.repository;

import com.hackerrank.hotel.model.Hotel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {

    List<Hotel> findByCityIdAndDeletedFalse(Long cityId);
}
