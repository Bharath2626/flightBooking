package com.flight.booking.exception;

/** Thrown when one or more requested seats are already taken (non-retryable). */
public class SeatUnavailableException extends RuntimeException {
    public SeatUnavailableException(String message) {
        super(message);
    }
}
