package com.flight.booking.repository;

import com.flight.booking.domain.entity.BookingDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingDetailsRepository extends JpaRepository<BookingDetails, Long> {

    List<BookingDetails> findByProfileId(Long profileId);

    /** A user's bookings with seats + flight details eagerly loaded (newest first). */
    @Query("select distinct b from BookingDetails b " +
            "left join fetch b.bookingSeats bs " +
            "left join fetch bs.seat s " +
            "left join fetch s.flightScheduled fs " +
            "left join fetch fs.flight " +
            "where b.profile.id = :profileId " +
            "order by b.id desc")
    List<BookingDetails> findByProfileWithSeats(@Param("profileId") Long profileId);
}
