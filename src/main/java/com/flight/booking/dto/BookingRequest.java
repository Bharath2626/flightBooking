package com.flight.booking.dto;

import com.flight.booking.domain.enums.PaymentMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Body for POST /api/bookings — one passenger per selected seat, plus intended payment mode. */
public record BookingRequest(
        @NotEmpty(message = "at least one passenger/seat is required")
        @Valid
        List<PassengerRequest> passengers,
        PaymentMode paymentMode
) {}
