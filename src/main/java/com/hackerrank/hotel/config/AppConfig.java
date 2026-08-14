package com.hackerrank.hotel.config;

import com.hackerrank.hotel.repository.CityRepository;
import com.hackerrank.hotel.repository.HotelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Central switchboard for cross-cutting framework features. Each @Enable*
 * annotation activates an entire subsystem that its matching annotations
 * (@Cacheable, @Scheduled, @Async, @CreatedDate) silently depend on —
 * a favorite interview trap: "@Scheduled does nothing without @EnableScheduling".
 */
@Configuration
@EnableCaching
@EnableScheduling
@EnableAsync
@EnableJpaAuditing
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    /**
     * Dev-only startup report. @Profile("!prod") means this bean simply does
     * not exist when the prod profile is active. CommandLineRunner beans run
     * once, after the context is fully started.
     */
    @Bean
    @Profile("!prod")
    public CommandLineRunner seedDataReport(CityRepository cities, HotelRepository hotels) {
        return args -> log.info("Seed data loaded: {} cities, {} hotels ({} active)",
                cities.count(), hotels.count(), hotels.countActive());
    }
}
