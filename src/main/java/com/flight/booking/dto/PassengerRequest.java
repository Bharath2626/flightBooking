package com.flight.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** One passenger assigned to one seat. */
public record PassengerRequest(
        @NotNull Long seatId,
        @NotBlank String firstName,
        String lastName,
        Integer age,
        String gender
) {}
