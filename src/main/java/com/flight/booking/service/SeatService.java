package com.flight.booking.service;

import com.flight.booking.dto.SeatDto;
import com.flight.booking.exception.ResourceNotFoundException;
import com.flight.booking.repository.FlightScheduledRepository;
import com.flight.booking.repository.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class SeatService {

    private final SeatRepository seatRepository;
    private final FlightScheduledRepository scheduledRepository;

    public SeatService(SeatRepository seatRepository, FlightScheduledRepository scheduledRepository) {
        this.seatRepository = seatRepository;
        this.scheduledRepository = scheduledRepository;
    }

    /** Seat map for a scheduled flight. {@code onlyAvailable} filters to bookable seats. */
    @Transactional(readOnly = true)
    public List<SeatDto> seatsForFlight(Long scheduledId, boolean onlyAvailable) {
        if (!scheduledRepository.existsById(scheduledId)) {
            throw new ResourceNotFoundException("Scheduled flight " + scheduledId + " not found");
        }
        List<com.flight.booking.domain.entity.Seat> seats = onlyAvailable
                ? seatRepository.findByFlightScheduledIdAndAvailableTrue(scheduledId)
                : seatRepository.findByFlightScheduledId(scheduledId);
        return seats.stream()
                .sorted(Comparator.comparing(com.flight.booking.domain.entity.Seat::getSeatNo))
                .map(s -> new SeatDto(s.getId(), s.getSeatNo(), s.getSeatType(),
                        s.getClassType(), s.isAvailable()))
                .toList();
    }
}
