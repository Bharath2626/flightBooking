package com.flight.booking.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A precomputed journey from origin to destination: one leg (direct) or several
 * (connecting). {@code availableSeats} is the minimum across legs, so the whole
 * itinerary is bookable for that many passengers.
 */
public record Itinerary(
        boolean direct,
        int stops,
        String fromAirport,
        String toAirport,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        long totalDurationMinutes,
        BigDecimal totalPrice,
        int availableSeats,
        List<Leg> legs
) {}
