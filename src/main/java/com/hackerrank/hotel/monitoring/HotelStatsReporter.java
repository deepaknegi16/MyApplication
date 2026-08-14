package com.hackerrank.hotel.monitoring;

import com.hackerrank.hotel.repository.HotelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic background job (@Scheduled needs @EnableScheduling). The whole
 * bean is a feature flag: @ConditionalOnProperty removes it from the context
 * entirely when app.stats.enabled=false — Boot's own auto-configuration is
 * built from exactly this kind of conditional annotation.
 */
@Component
@ConditionalOnProperty(name = "app.stats.enabled", havingValue = "true", matchIfMissing = true)
public class HotelStatsReporter {

    private static final Logger log = LoggerFactory.getLogger(HotelStatsReporter.class);

    private final HotelRepository hotelRepository;

    public HotelStatsReporter(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    @Scheduled(initialDelay = 60_000, fixedRate = 300_000)
    public void reportActiveHotels() {
        log.info("STATS: {} active hotels of {} total", hotelRepository.countActive(), hotelRepository.count());
    }
}
