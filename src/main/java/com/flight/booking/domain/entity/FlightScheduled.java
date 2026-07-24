package com.flight.booking.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A concrete, dated flight leg between two airports.
 * departureTime / arrivalTime are full timestamps so connecting-flight timing
 * (min connection, layover, midnight crossover) can be computed precisely.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "flights_scheduled", indexes = {
        @Index(name = "idx_sched_route_date", columnList = "from_airport,to_airport,flight_date"),
        @Index(name = "idx_sched_date", columnList = "flight_date")
})
public class FlightScheduled extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Column(name = "from_airport", nullable = false, length = 8)
    private String fromAirport;

    @Column(name = "to_airport", nullable = false, length = 8)
    private String toAirport;

    @Column(name = "flight_date", nullable = false)
    private LocalDate flightDate;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime arrivalTime;

    /** false once an admin cancels this scheduled leg. */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Transient
    public long durationMinutes() {
        return java.time.Duration.between(departureTime, arrivalTime).toMinutes();
    }
}
