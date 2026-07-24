package com.flight.booking.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A flight "product" (e.g. AI-202). Actual dated instances live in FlightScheduled. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "flight")
public class Flight extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flight_name", nullable = false)
    private String flightName;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
