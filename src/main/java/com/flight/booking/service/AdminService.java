package com.flight.booking.service;

import com.flight.booking.domain.entity.*;
import com.flight.booking.domain.enums.ClassType;
import com.flight.booking.domain.enums.SeatType;
import com.flight.booking.dto.CreateFlightRequest;
import com.flight.booking.dto.FlightScheduledResponse;
import com.flight.booking.exception.BadRequestException;
import com.flight.booking.exception.ResourceNotFoundException;
import com.flight.booking.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin operations. Every mutation invalidates the itinerary cache so search results
 * reflect the change immediately (in addition to the hourly scheduled refresh).
 */
@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);
    private static final SeatType[] SEAT_CYCLE =
            {SeatType.WINDOW, SeatType.MIDDLE, SeatType.AISLE};

    private final FlightRepository flightRepository;
    private final FlightScheduledRepository scheduledRepository;
    private final SeatRepository seatRepository;
    private final PricingRepository pricingRepository;
    private final ItineraryCache cache;

    public AdminService(FlightRepository flightRepository,
                        FlightScheduledRepository scheduledRepository,
                        SeatRepository seatRepository,
                        PricingRepository pricingRepository,
                        ItineraryCache cache) {
        this.flightRepository = flightRepository;
        this.scheduledRepository = scheduledRepository;
        this.seatRepository = seatRepository;
        this.pricingRepository = pricingRepository;
        this.cache = cache;
    }

    @Transactional
    public FlightScheduledResponse createFlight(CreateFlightRequest req) {
        if (!req.arrivalTime().isAfter(req.departureTime())) {
            throw new BadRequestException("arrivalTime must be after departureTime");
        }
        if (req.economySeats() + req.businessSeats() <= 0) {
            throw new BadRequestException("flight must have at least one seat");
        }
        if (req.businessSeats() > 0 && req.businessPrice() == null) {
            throw new BadRequestException("businessPrice is required when businessSeats > 0");
        }

        Flight flight = new Flight();
        flight.setFlightName(req.flightName());
        flight.setActive(true);
        flightRepository.save(flight);

        FlightScheduled scheduled = new FlightScheduled();
        scheduled.setFlight(flight);
        scheduled.setFromAirport(req.fromAirport().trim().toUpperCase());
        scheduled.setToAirport(req.toAirport().trim().toUpperCase());
        scheduled.setFlightDate(req.flightDate());
        scheduled.setDepartureTime(req.departureTime());
        scheduled.setArrivalTime(req.arrivalTime());
        scheduled.setActive(true);
        scheduledRepository.save(scheduled);

        List<Seat> seats = new ArrayList<>();
        seats.addAll(buildSeats(scheduled, ClassType.ECONOMY, "E", req.economySeats()));
        seats.addAll(buildSeats(scheduled, ClassType.BUSINESS, "B", req.businessSeats()));
        seatRepository.saveAll(seats);

        BigDecimal minPrice = req.economyPrice();
        if (req.businessPrice() != null) {
            minPrice = minPrice.min(req.businessPrice());
        }
        Pricing pricing = new Pricing();
        pricing.setFlightScheduled(scheduled);
        pricing.setMinPrice(minPrice);
        pricing.setPricesJson(buildPricesJson(req));
        pricingRepository.save(pricing);

        cache.invalidateAll();
        log.info("Admin created scheduled flight {} ({} -> {} on {}) with {} seats",
                scheduled.getId(), scheduled.getFromAirport(), scheduled.getToAirport(),
                scheduled.getFlightDate(), seats.size());

        return toResponse(scheduled, seats.size());
    }

    @Transactional
    public FlightScheduledResponse cancelFlight(Long scheduledId) {
        FlightScheduled scheduled = scheduledRepository.findById(scheduledId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled flight " + scheduledId + " not found"));
        scheduled.setActive(false);
        scheduledRepository.save(scheduled);

        // Take the seats out of inventory so nothing new can be booked on a canceled flight.
        List<Seat> seats = seatRepository.findByFlightScheduledId(scheduledId);
        seats.forEach(s -> s.setAvailable(false));
        seatRepository.saveAll(seats);

        cache.invalidateAll();
        log.info("Admin canceled scheduled flight {}", scheduledId);
        return toResponse(scheduled, seats.size());
    }

    private List<Seat> buildSeats(FlightScheduled scheduled, ClassType classType, String prefix, int count) {
        List<Seat> seats = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            Seat seat = new Seat();
            seat.setSeatNo(prefix + i);
            seat.setFlightScheduled(scheduled);
            seat.setClassType(classType);
            seat.setSeatType(SEAT_CYCLE[(i - 1) % SEAT_CYCLE.length]);
            seat.setAvailable(true);
            seats.add(seat);
        }
        return seats;
    }

    private String buildPricesJson(CreateFlightRequest req) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"ECONOMY\":").append(req.economyPrice().toPlainString());
        if (req.businessPrice() != null) {
            sb.append(",\"BUSINESS\":").append(req.businessPrice().toPlainString());
        }
        return sb.append("}").toString();
    }

    private FlightScheduledResponse toResponse(FlightScheduled fs, int totalSeats) {
        return new FlightScheduledResponse(
                fs.getId(), fs.getFlight().getId(), fs.getFlight().getFlightName(),
                fs.getFromAirport(), fs.getToAirport(), fs.getFlightDate(),
                fs.getDepartureTime(), fs.getArrivalTime(), fs.isActive(), totalSeats);
    }
}
