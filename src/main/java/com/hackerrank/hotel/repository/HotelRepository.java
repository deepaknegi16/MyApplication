package com.hackerrank.hotel.repository;

import com.hackerrank.hotel.model.Hotel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {

    // derived query: the method name is parsed into "where city_id = ? and deleted = false"
    List<Hotel> findByCityIdAndDeletedFalse(Long cityId);

    // case-insensitive substring match on the name, across all cities
    List<Hotel> findByDeletedFalseAndNameContainingIgnoreCaseOrderByNameAsc(String name);

    // hand-written JPQL for when a method name would get unreadable
    @Query("select count(h) from Hotel h where h.deleted = false")
    long countActive();

    @Query("select count(h) from Hotel h where h.deleted = false and h.city.id = :cityId")
    long countActiveInCity(@Param("cityId") Long cityId);
}
