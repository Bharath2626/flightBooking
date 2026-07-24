package com.flight.booking.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Join between a booking and each seat it holds. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "booking_seats",
        indexes = {
                @Index(name = "idx_bs_booking", columnList = "booking_id"),
                @Index(name = "idx_bs_seat", columnList = "seat_id")
        })
public class BookingSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private BookingDetails booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    // Passenger travelling in this seat.
    @Column(name = "passenger_first_name")
    private String passengerFirstName;

    @Column(name = "passenger_last_name")
    private String passengerLastName;

    @Column(name = "passenger_age")
    private Integer passengerAge;

    @Column(name = "passenger_gender", length = 16)
    private String passengerGender;
}
