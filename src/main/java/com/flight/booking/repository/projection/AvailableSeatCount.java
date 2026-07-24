package com.flight.booking.repository.projection;

/** Projection: number of available seats per scheduled flight. */
public interface AvailableSeatCount {
    Long getScheduledId();
    Long getSeatCount();
}
