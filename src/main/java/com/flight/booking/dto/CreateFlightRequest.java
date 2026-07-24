package com.flight.booking.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Admin payload to schedule a new flight together with its seats and pricing. */
public record CreateFlightRequest(
        @NotBlank String flightName,
        @NotBlank String fromAirport,
        @NotBlank String toAirport,
        @NotNull LocalDate flightDate,
        @NotNull LocalDateTime departureTime,
        @NotNull LocalDateTime arrivalTime,
        @Min(0) int economySeats,
        @Min(0) int businessSeats,
        @NotNull @DecimalMin("0.0") BigDecimal economyPrice,
        BigDecimal businessPrice
) {}
