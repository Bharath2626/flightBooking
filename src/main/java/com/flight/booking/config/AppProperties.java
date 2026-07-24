package com.flight.booking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Central holder for tunable business rules loaded from application.properties. */
@Component
public class AppProperties {

    @Value("${app.cache.refresh-interval-ms:3600000}")
    private long cacheRefreshIntervalMs;

    @Value("${app.itinerary.max-legs:3}")
    private int maxLegs;

    @Value("${app.itinerary.min-connection-minutes:45}")
    private long minConnectionMinutes;

    @Value("${app.itinerary.max-layover-minutes:360}")
    private long maxLayoverMinutes;

    @Value("${app.itinerary.max-itineraries:300}")
    private int maxItineraries;

    @Value("${app.booking.max-retries:3}")
    private int bookingMaxRetries;

    @Value("${app.booking.retry-backoff-ms:50}")
    private long bookingRetryBackoffMs;

    public long getCacheRefreshIntervalMs() { return cacheRefreshIntervalMs; }
    public int getMaxLegs() { return maxLegs; }
    public long getMinConnectionMinutes() { return minConnectionMinutes; }
    public long getMaxLayoverMinutes() { return maxLayoverMinutes; }
    public int getMaxItineraries() { return maxItineraries; }
    public int getBookingMaxRetries() { return bookingMaxRetries; }
    public long getBookingRetryBackoffMs() { return bookingRetryBackoffMs; }
}
