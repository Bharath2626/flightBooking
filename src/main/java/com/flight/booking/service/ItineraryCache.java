package com.flight.booking.service;

import com.flight.booking.model.Itinerary;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory precomputed-itinerary cache backed by a {@link ConcurrentHashMap}, keyed by
 * (from, to, date). Search reads from here so sorting and pagination are pure in-memory ops.
 *
 * Freshness is maintained two ways, exactly as required:
 *   1. a scheduled job rebuilds every cached route every hour;
 *   2. {@link #invalidateAll()} is called whenever an admin adds or cancels a flight.
 */
@Component
public class ItineraryCache {

    private static final Logger log = LoggerFactory.getLogger(ItineraryCache.class);

    private final ItineraryBuilder builder;
    private final Map<RouteKey, List<Itinerary>> cache = new ConcurrentHashMap<>();

    public ItineraryCache(ItineraryBuilder builder) {
        this.builder = builder;
    }

    /** Cache key. Airport codes are normalised to upper-case so lookups are case-insensitive. */
    public record RouteKey(String from, String to, LocalDate date) {
        public static RouteKey of(String from, String to, LocalDate date) {
            return new RouteKey(from.trim().toUpperCase(), to.trim().toUpperCase(), date);
        }
    }

    /** Returns the precomputed itineraries for a route, building and caching them on first miss. */
    public List<Itinerary> get(String from, String to, LocalDate date) {
        RouteKey key = RouteKey.of(from, to, date);
        List<Itinerary> hit = cache.get(key);
        if (hit != null) {
            return hit;
        }
        // Build outside any map lock; a rare duplicate build under race is harmless.
        List<Itinerary> built = builder.build(key.from(), key.to(), key.date());
        cache.put(key, built);
        log.debug("Cache miss -> built {} itineraries for {}", built.size(), key);
        return built;
    }

    public boolean isCached(String from, String to, LocalDate date) {
        return cache.containsKey(RouteKey.of(from, to, date));
    }

    /** Clears everything. Called on any admin flight change so stale routes are never served. */
    public void invalidateAll() {
        int size = cache.size();
        cache.clear();
        log.info("Itinerary cache invalidated ({} routes cleared)", size);
    }

    /** Rebuilds a single cached route in place. */
    private void rebuild(RouteKey key) {
        try {
            cache.put(key, builder.build(key.from(), key.to(), key.date()));
        } catch (RuntimeException ex) {
            log.warn("Failed to rebuild cache for {}: {}", key, ex.getMessage());
        }
    }

    /**
     * Hourly full refresh of every route currently in the cache. Fixed-rate is driven by
     * the configured interval (default 3,600,000 ms = 1 hour).
     */
    @Scheduled(fixedRateString = "${app.cache.refresh-interval-ms:3600000}")
    public void scheduledRefresh() {
        if (cache.isEmpty()) {
            return;
        }
        log.info("Scheduled refresh of {} cached routes starting", cache.size());
        cache.keySet().stream().filter(Objects::nonNull).forEach(this::rebuild);
        log.info("Scheduled refresh complete");
    }

    @PostConstruct
    void started() {
        log.info("ItineraryCache ready (hourly refresh + invalidate-on-admin-change)");
    }
}
