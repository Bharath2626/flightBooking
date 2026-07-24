package com.flight.booking.repository;

import com.flight.booking.domain.entity.Seat;
import com.flight.booking.repository.projection.AvailableSeatCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByFlightScheduledIdAndAvailableTrue(Long flightScheduledId);

    List<Seat> findByFlightScheduledId(Long flightScheduledId);

    /** Loads seats with their scheduled flight so we can read prices before reserving. */
    @Query("select s from Seat s join fetch s.flightScheduled where s.id in :ids")
    List<Seat> findAllWithScheduleByIds(@Param("ids") List<Long> ids);

    /** Available-seat counts for a set of scheduled flights, used while precomputing itineraries. */
    @Query("select s.flightScheduled.id as scheduledId, count(s) as seatCount " +
            "from Seat s where s.available = true and s.flightScheduled.id in :ids " +
            "group by s.flightScheduled.id")
    List<AvailableSeatCount> countAvailableByScheduledIds(@Param("ids") List<Long> ids);

    /**
     * Atomically reserve a single seat. Succeeds (returns 1) only if the seat was still
     * available; returns 0 if another transaction already took it. This conditional
     * UPDATE is the core double-booking guard — the {@code version} bump keeps optimistic
     * locking consistent for any concurrently-loaded entity copies.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Seat s set s.available = false, s.version = s.version + 1 " +
            "where s.id = :seatId and s.available = true")
    int reserveSeat(@Param("seatId") Long seatId);

    /** Release a seat (used on cancellation / rollback). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Seat s set s.available = true, s.version = s.version + 1 " +
            "where s.id = :seatId and s.available = false")
    int releaseSeat(@Param("seatId") Long seatId);
}
