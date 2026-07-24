package com.flight.booking.config;

import com.flight.booking.domain.entity.*;
import com.flight.booking.domain.enums.ClassType;
import com.flight.booking.domain.enums.Role;
import com.flight.booking.domain.enums.SeatType;
import com.flight.booking.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds on first boot (each part independently, only when empty):
 *   - two users (admin + regular),
 *   - the airport master list,
 *   - a hub-and-spoke flight network so flights exist "in all directions":
 *       hub<->hub is direct; spoke<->spoke / spoke<->non-gateway-hub resolves as connecting.
 * Skipped under the test profile.
 */
@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final List<LocalDate> DATES = List.of(
            LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2));
    private static final int[] DEPARTURE_HOURS = {7, 13};   // two waves/day -> enables connections
    private static final int FLIGHT_MINUTES = 135;
    private static final int ECONOMY_SEATS = 12;
    private static final int BUSINESS_SEATS = 4;
    private static final SeatType[] SEAT_CYCLE = {SeatType.WINDOW, SeatType.MIDDLE, SeatType.AISLE};

    // 6 hubs (full mesh, direct) + 14 spokes (each linked to the two gateway hubs).
    private static final List<String> HUBS = List.of("DEL", "BOM", "BLR", "MAA", "CCU", "HYD");
    private static final List<String> SPOKES = List.of(
            "AMD", "PNQ", "GOI", "COK", "JAI", "LKO", "PAT", "GAU", "BBI", "NAG", "IXC", "TRV", "VNS", "ATQ");
    private static final List<String> GATEWAYS = List.of("DEL", "BOM");

    private final UserProfileRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AirportRepository airportRepository;
    private final FlightRepository flightRepository;
    private final FlightScheduledRepository scheduledRepository;
    private final SeatRepository seatRepository;
    private final PricingRepository pricingRepository;

    public DataSeeder(UserProfileRepository userRepository, PasswordEncoder passwordEncoder,
                      AirportRepository airportRepository, FlightRepository flightRepository,
                      FlightScheduledRepository scheduledRepository, SeatRepository seatRepository,
                      PricingRepository pricingRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.airportRepository = airportRepository;
        this.flightRepository = flightRepository;
        this.scheduledRepository = scheduledRepository;
        this.seatRepository = seatRepository;
        this.pricingRepository = pricingRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() == 0) {
            createUser("Admin", "admin@flight.com", "admin123", Role.ADMIN);
            createUser("Test", "user@flight.com", "user123", Role.USER);
            log.info("Seeded users: admin@flight.com/admin123, user@flight.com/user123");
        }
        if (airportRepository.count() == 0) {
            airportRepository.saveAll(airports());
            log.info("Seeded {} airports", HUBS.size() + SPOKES.size());
        }
        if (scheduledRepository.count() == 0) {
            seedNetwork();
        }
    }

    private void createUser(String name, String email, String rawPassword, Role role) {
        UserProfile u = new UserProfile();
        u.setFName(name);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setRole(role);
        userRepository.save(u);
    }

    private List<Airport> airports() {
        return List.of(
                new Airport("DEL", "Delhi", "Indira Gandhi International", "Delhi"),
                new Airport("BOM", "Mumbai", "Chhatrapati Shivaji Maharaj International", "Maharashtra"),
                new Airport("BLR", "Bengaluru", "Kempegowda International", "Karnataka"),
                new Airport("MAA", "Chennai", "Chennai International", "Tamil Nadu"),
                new Airport("CCU", "Kolkata", "Netaji Subhas Chandra Bose International", "West Bengal"),
                new Airport("HYD", "Hyderabad", "Rajiv Gandhi International", "Telangana"),
                new Airport("AMD", "Ahmedabad", "Sardar Vallabhbhai Patel International", "Gujarat"),
                new Airport("PNQ", "Pune", "Pune Airport", "Maharashtra"),
                new Airport("GOI", "Goa", "Dabolim Airport", "Goa"),
                new Airport("COK", "Kochi", "Cochin International", "Kerala"),
                new Airport("JAI", "Jaipur", "Jaipur International", "Rajasthan"),
                new Airport("LKO", "Lucknow", "Chaudhary Charan Singh International", "Uttar Pradesh"),
                new Airport("PAT", "Patna", "Jay Prakash Narayan International", "Bihar"),
                new Airport("GAU", "Guwahati", "Lokpriya Gopinath Bordoloi International", "Assam"),
                new Airport("BBI", "Bhubaneswar", "Biju Patnaik International", "Odisha"),
                new Airport("NAG", "Nagpur", "Dr. Babasaheb Ambedkar International", "Maharashtra"),
                new Airport("IXC", "Chandigarh", "Chandigarh Airport", "Chandigarh"),
                new Airport("TRV", "Thiruvananthapuram", "Trivandrum International", "Kerala"),
                new Airport("VNS", "Varanasi", "Lal Bahadur Shastri International", "Uttar Pradesh"),
                new Airport("ATQ", "Amritsar", "Sri Guru Ram Dass Jee International", "Punjab"));
    }

    private void seedNetwork() {
        // Build the directed route list.
        List<String[]> routes = new ArrayList<>();
        for (String a : HUBS) {
            for (String b : HUBS) {
                if (!a.equals(b)) routes.add(new String[]{a, b});
            }
        }
        for (String spoke : SPOKES) {
            for (String hub : GATEWAYS) {
                routes.add(new String[]{spoke, hub});
                routes.add(new String[]{hub, spoke});
            }
        }

        List<Flight> flights = new ArrayList<>();
        List<FlightScheduled> scheduleds = new ArrayList<>();
        List<Seat> seats = new ArrayList<>();
        List<Pricing> pricings = new ArrayList<>();

        int seq = 100;
        for (LocalDate date : DATES) {
            for (String[] r : routes) {
                for (int hour : DEPARTURE_HOURS) {
                    seq++;
                    Flight flight = new Flight();
                    flight.setFlightName("AI" + seq);
                    flight.setActive(true);
                    flights.add(flight);

                    FlightScheduled fs = new FlightScheduled();
                    fs.setFlight(flight);
                    fs.setFromAirport(r[0]);
                    fs.setToAirport(r[1]);
                    fs.setFlightDate(date);
                    fs.setDepartureTime(date.atTime(hour, 0));
                    fs.setArrivalTime(date.atTime(hour, 0).plusMinutes(FLIGHT_MINUTES));
                    fs.setActive(true);
                    scheduleds.add(fs);

                    BigDecimal eco = fareFor(r[0], r[1]);
                    BigDecimal biz = eco.add(BigDecimal.valueOf(3500));
                    Pricing pricing = new Pricing();
                    pricing.setFlightScheduled(fs);
                    pricing.setMinPrice(eco);
                    pricing.setPricesJson("{\"ECONOMY\":" + eco.toPlainString()
                            + ",\"BUSINESS\":" + biz.toPlainString() + "}");
                    pricings.add(pricing);

                    seats.addAll(buildSeats(fs, ClassType.ECONOMY, "E", ECONOMY_SEATS));
                    seats.addAll(buildSeats(fs, ClassType.BUSINESS, "B", BUSINESS_SEATS));
                }
            }
        }

        flightRepository.saveAll(flights);
        scheduledRepository.saveAll(scheduleds);
        seatRepository.saveAll(seats);
        pricingRepository.saveAll(pricings);
        log.info("Seeded flight network: {} flights, {} seats across {} dates ({} routes/date x {} waves)",
                scheduleds.size(), seats.size(), DATES.size(), routes.size(), DEPARTURE_HOURS.length);
    }

    private List<Seat> buildSeats(FlightScheduled fs, ClassType classType, String prefix, int count) {
        List<Seat> list = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            Seat s = new Seat();
            s.setSeatNo(prefix + i);
            s.setFlightScheduled(fs);
            s.setClassType(classType);
            s.setSeatType(SEAT_CYCLE[(i - 1) % SEAT_CYCLE.length]);
            s.setAvailable(true);
            list.add(s);
        }
        return list;
    }

    /** Deterministic fare (2500-4999) so results are stable across restarts. */
    private BigDecimal fareFor(String from, String to) {
        int h = Math.abs((from + "-" + to).hashCode());
        return BigDecimal.valueOf(2500 + (h % 2500));
    }
}
