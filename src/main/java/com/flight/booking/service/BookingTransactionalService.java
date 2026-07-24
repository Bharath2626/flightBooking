package com.flight.booking.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flight.booking.domain.entity.*;
import com.flight.booking.domain.enums.BookingStatus;
import com.flight.booking.domain.enums.ClassType;
import com.flight.booking.domain.enums.PaymentMode;
import com.flight.booking.domain.enums.PaymentStatus;
import com.flight.booking.dto.BookingResponse;
import com.flight.booking.dto.PassengerRequest;
import com.flight.booking.exception.BadRequestException;
import com.flight.booking.exception.ResourceNotFoundException;
import com.flight.booking.exception.SeatUnavailableException;
import com.flight.booking.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The transactional heart of booking. Reserves every requested seat with an atomic
 * conditional UPDATE; if any seat is already taken the whole transaction rolls back,
 * so a booking is strictly all-or-nothing and can never double-book a seat.
 */
@Service
public class BookingTransactionalService {

    private static final Logger log = LoggerFactory.getLogger(BookingTransactionalService.class);

    private final UserProfileRepository userRepository;
    private final SeatRepository seatRepository;
    private final PricingRepository pricingRepository;
    private final BookingDetailsRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    // Thread-safe once configured; used to parse the small prices_json fare map.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public BookingTransactionalService(UserProfileRepository userRepository,
                                       SeatRepository seatRepository,
                                       PricingRepository pricingRepository,
                                       BookingDetailsRepository bookingRepository,
                                       PaymentRepository paymentRepository) {
        this.userRepository = userRepository;
        this.seatRepository = seatRepository;
        this.pricingRepository = pricingRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
    }

    /** Seat data captured before the reserving UPDATEs clear the persistence context. */
    private record SeatInfo(Long id, String seatNo, Long scheduledId, ClassType classType) {}

    @Transactional
    public BookingResponse reserveAndBook(Long profileId, List<PassengerRequest> passengers, PaymentMode mode) {
        Map<Long, PassengerRequest> passengerBySeat = passengers.stream()
                .collect(Collectors.toMap(PassengerRequest::seatId, p -> p, (a, b) -> a));
        List<Long> ids = passengerBySeat.keySet().stream().toList();
        if (ids.isEmpty()) {
            throw new BadRequestException("No seats selected");
        }
        if (!userRepository.existsById(profileId)) {
            throw new ResourceNotFoundException("User profile " + profileId + " not found");
        }

        List<Seat> seats = seatRepository.findAllWithScheduleByIds(ids);
        if (seats.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more seats do not exist");
        }

        List<SeatInfo> infos = seats.stream()
                .map(s -> new SeatInfo(s.getId(), s.getSeatNo(),
                        s.getFlightScheduled().getId(), s.getClassType()))
                .toList();

        // Per-flight fare table {classType -> fare}, parsed from Pricing.prices_json.
        Map<Long, Map<ClassType, BigDecimal>> faresByScheduled = loadFares(
                infos.stream().map(SeatInfo::scheduledId).distinct().toList());

        // Atomic reservation. A returned count != 1 means the seat was taken by a concurrent
        // booking -> throw so the whole transaction rolls back (releasing any we just took).
        for (SeatInfo si : infos) {
            int reserved = seatRepository.reserveSeat(si.id());
            if (reserved != 1) {
                throw new SeatUnavailableException("Seat " + si.seatNo() + " is no longer available");
            }
        }

        // Charge each seat its own class fare (business != economy).
        BigDecimal amount = infos.stream()
                .map(si -> fareFor(faresByScheduled.get(si.scheduledId()), si.classType()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        UserProfile profileRef = userRepository.getReferenceById(profileId);

        BookingDetails booking = new BookingDetails();
        booking.setProfile(profileRef);
        booking.setStatus(BookingStatus.PENDING); // becomes CONFIRMED once payment succeeds
        for (SeatInfo si : infos) {
            BookingSeat bs = new BookingSeat();
            bs.setBooking(booking);
            bs.setSeat(seatRepository.getReferenceById(si.id()));
            PassengerRequest p = passengerBySeat.get(si.id());
            if (p != null) {
                bs.setPassengerFirstName(p.firstName());
                bs.setPassengerLastName(p.lastName());
                bs.setPassengerAge(p.age());
                bs.setPassengerGender(p.gender());
            }
            booking.getBookingSeats().add(bs);
        }
        bookingRepository.save(booking);

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setProfile(profileRef);
        payment.setAmountToBePaid(amount);
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setModeOfPayment(mode);
        paymentRepository.save(payment);

        List<String> seatNumbers = infos.stream().map(SeatInfo::seatNo).toList();
        return new BookingResponse(booking.getId(), booking.getStatus(), seatNumbers,
                amount, payment.getId(), payment.getStatus());
    }

    private Map<Long, Map<ClassType, BigDecimal>> loadFares(List<Long> scheduledIds) {
        Map<Long, Map<ClassType, BigDecimal>> result = new HashMap<>();
        for (Pricing p : pricingRepository.findByFlightScheduledIdIn(scheduledIds)) {
            result.put(p.getFlightScheduled().getId(), parseFares(p));
        }
        return result;
    }

    /** Parses {"ECONOMY":4500,"BUSINESS":9000} -> a class-keyed fare map. */
    private Map<ClassType, BigDecimal> parseFares(Pricing pricing) {
        Map<ClassType, BigDecimal> fares = new HashMap<>();
        String json = pricing.getPricesJson();
        if (json != null && !json.isBlank()) {
            try {
                Map<String, BigDecimal> raw = OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
                raw.forEach((k, v) -> {
                    try {
                        fares.put(ClassType.valueOf(k.trim().toUpperCase()), v);
                    } catch (IllegalArgumentException ignored) {
                        // unknown class key in JSON — skip
                    }
                });
            } catch (Exception ex) {
                log.warn("Could not parse prices_json for scheduled flight {}: {}",
                        pricing.getFlightScheduled().getId(), ex.getMessage());
            }
        }
        // Always keep min_price available as a fallback fare.
        fares.putIfAbsent(null, pricing.getMinPrice());
        return fares;
    }

    private BigDecimal fareFor(Map<ClassType, BigDecimal> fares, ClassType classType) {
        if (fares == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal fare = fares.get(classType);
        if (fare != null) {
            return fare;
        }
        // fall back to min_price (stored under the null key), then zero
        BigDecimal fallback = fares.get(null);
        return fallback != null ? fallback : BigDecimal.ZERO;
    }
}
