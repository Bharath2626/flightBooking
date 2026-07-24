package com.flight.booking.domain.entity;

import com.flight.booking.domain.enums.ClassType;
import com.flight.booking.domain.enums.SeatType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single seat on a scheduled flight. The {@code available} flag plus the JPA
 * {@code @Version} column give us optimistic locking so two concurrent bookings
 * of the same seat cannot both succeed (double-booking protection).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "seats",
        uniqueConstraints = @UniqueConstraint(name = "uk_seat_flight_no",
                columnNames = {"flight_scheduled_id", "seat_no"}),
        indexes = @Index(name = "idx_seat_flight_avail", columnList = "flight_scheduled_id,is_available"))
public class Seat extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seat_no", nullable = false, length = 8)
    private String seatNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flight_scheduled_id", nullable = false)
    private FlightScheduled flightScheduled;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 20)
    private SeatType seatType;

    @Enumerated(EnumType.STRING)
    @Column(name = "class_type", nullable = false, length = 20)
    private ClassType classType;

    @Column(name = "is_available", nullable = false)
    private boolean available = true;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
