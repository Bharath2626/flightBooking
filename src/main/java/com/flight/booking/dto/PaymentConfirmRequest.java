package com.flight.booking.dto;

import com.flight.booking.domain.enums.PaymentMode;
import jakarta.validation.constraints.NotNull;

/**
 * Result of the payment attempt reported back by the (simulated) gateway.
 * success=true -> payment SUCCESS + booking CONFIRMED;
 * success=false -> payment FAILED + seats released + booking CANCELED.
 */
public record PaymentConfirmRequest(
        @NotNull Boolean success,
        PaymentMode modeOfPayment
) {}
