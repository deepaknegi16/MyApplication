package com.hackerrank.hotel.event;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Decoupled audit trail: the service publishes an event and moves on; this
 * listener reacts on a separate thread (@Async needs @EnableAsync), so slow
 * audit sinks (a real system might call an audit API) never delay the
 * DELETE response. Variant worth knowing: @TransactionalEventListener fires
 * only after the surrounding transaction commits.
 */
@Component
public class HotelAuditListener {

    private static final Logger log = LoggerFactory.getLogger(HotelAuditListener.class);

    @PostConstruct
    void init() {
        // lifecycle callback: runs once after dependency injection completes
        log.info("Hotel audit listener ready");
    }

    @Async
    @EventListener
    public void onHotelDeleted(HotelDeletedEvent event) {
        log.info("AUDIT: hotel {} ('{}') soft-deleted by '{}' [thread={}]",
                event.hotelId(), event.hotelName(), event.deletedBy(), Thread.currentThread().getName());
    }
}
