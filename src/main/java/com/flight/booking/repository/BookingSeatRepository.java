package com.flight.booking.repository;

import com.flight.booking.domain.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    /** Seat ids held by a booking — used to release seats on cancellation / payment failure. */
    @Query("select bs.seat.id from BookingSeat bs where bs.booking.id = :bookingId")
    List<Long> findSeatIdsByBookingId(@Param("bookingId") Long bookingId);
}
