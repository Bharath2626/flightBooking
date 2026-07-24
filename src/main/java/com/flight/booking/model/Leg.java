package com.flight.booking.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One flight segment inside an itinerary (immutable, cache-friendly). */
public record Leg(
        Long scheduledId,
        Long flightId,
        String flightName,
        String fromAirport,
        String toAirport,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        long durationMinutes,
        BigDecimal price,
        int availableSeats
) {}
