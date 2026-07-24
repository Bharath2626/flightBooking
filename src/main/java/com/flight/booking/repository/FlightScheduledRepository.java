package com.flight.booking.repository;

import com.flight.booking.domain.entity.FlightScheduled;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FlightScheduledRepository extends JpaRepository<FlightScheduled, Long> {

    /** All active legs on a date — the raw material for building the itinerary graph. */
    @Query("select fs from FlightScheduled fs join fetch fs.flight " +
            "where fs.flightDate = :date and fs.active = true")
    List<FlightScheduled> findActiveByDate(@Param("date") LocalDate date);

    /** Distinct travel dates that currently have active scheduled flights (drives cache warm-up). */
    @Query("select distinct fs.flightDate from FlightScheduled fs where fs.active = true")
    List<LocalDate> findActiveDates();
}
