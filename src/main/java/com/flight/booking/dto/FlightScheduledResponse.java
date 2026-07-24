package com.flight.booking.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FlightScheduledResponse(
        Long scheduledId,
        Long flightId,
        String flightName,
        String fromAirport,
        String toAirport,
        LocalDate flightDate,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        boolean active,
        int totalSeats
) {}
