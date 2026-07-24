package com.flight.booking.service;

import com.flight.booking.config.AppProperties;
import com.flight.booking.domain.entity.FlightScheduled;
import com.flight.booking.domain.entity.Pricing;
import com.flight.booking.model.Itinerary;
import com.flight.booking.model.Leg;
import com.flight.booking.repository.FlightScheduledRepository;
import com.flight.booking.repository.PricingRepository;
import com.flight.booking.repository.SeatRepository;
import com.flight.booking.repository.projection.AvailableSeatCount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds all origin->destination itineraries (direct + connecting up to the configured
 * leg limit) for a given date from raw scheduled-flight data. This is the expensive step
 * whose output {@link ItineraryCache} stores so search/sort/paginate stay cheap.
 */
@Service
public class ItineraryBuilder {

    private final FlightScheduledRepository scheduledRepository;
    private final SeatRepository seatRepository;
    private final PricingRepository pricingRepository;
    private final AppProperties props;

    public ItineraryBuilder(FlightScheduledRepository scheduledRepository,
                            SeatRepository seatRepository,
                            PricingRepository pricingRepository,
                            AppProperties props) {
        this.scheduledRepository = scheduledRepository;
        this.seatRepository = seatRepository;
        this.pricingRepository = pricingRepository;
        this.props = props;
    }

    /**
     * @return every bookable-shaped itinerary from {@code from} to {@code to} on {@code date},
     *         each carrying its own available-seat count (seat filtering happens at query time).
     */
    @Transactional(readOnly = true)
    public List<Itinerary> build(String from, String to, LocalDate date) {
        List<FlightScheduled> legs = scheduledRepository.findActiveByDate(date);
        if (legs.isEmpty()) {
            return List.of();
        }

        List<Long> ids = legs.stream().map(FlightScheduled::getId).toList();

        Map<Long, Integer> seatCounts = new HashMap<>();
        for (AvailableSeatCount c : seatRepository.countAvailableByScheduledIds(ids)) {
            seatCounts.put(c.getScheduledId(), c.getSeatCount().intValue());
        }

        Map<Long, BigDecimal> prices = pricingRepository.findByFlightScheduledIdIn(ids).stream()
                .collect(Collectors.toMap(p -> p.getFlightScheduled().getId(), Pricing::getMinPrice));

        // adjacency: departures grouped by origin airport
        Map<String, List<FlightScheduled>> adjacency = legs.stream()
                .collect(Collectors.groupingBy(FlightScheduled::getFromAirport));

        List<List<FlightScheduled>> paths = bfs(from, to, adjacency);

        List<Itinerary> result = new ArrayList<>(paths.size());
        for (List<FlightScheduled> path : paths) {
            result.add(toItinerary(path, seatCounts, prices));
        }
        // default ordering: cheapest first (query layer re-sorts as requested)
        result.sort(Comparator.comparing(Itinerary::totalPrice));
        return result;
    }

    /**
     * Breadth-first enumeration of origin->destination itineraries. Because BFS expands
     * level by level, itineraries are discovered in increasing number of legs (direct
     * first, then 1-stop, then 2-stop), so when the {@code maxItineraries} cap is hit the
     * itineraries kept are the ones with the fewest stops. Each queue entry is a partial
     * path; a path is recorded when its last leg lands at the destination and is otherwise
     * extended with time-feasible, non-revisiting next legs up to {@code maxLegs}.
     */
    private List<List<FlightScheduled>> bfs(String from, String to,
                                            Map<String, List<FlightScheduled>> adjacency) {
        List<List<FlightScheduled>> out = new ArrayList<>();
        Deque<List<FlightScheduled>> queue = new ArrayDeque<>();

        // seed with every first leg departing the origin
        for (FlightScheduled first : adjacency.getOrDefault(from, List.of())) {
            List<FlightScheduled> seed = new ArrayList<>(1);
            seed.add(first);
            queue.add(seed);
        }

        while (!queue.isEmpty()) {
            if (out.size() >= props.getMaxItineraries()) {
                break;
            }
            List<FlightScheduled> path = queue.poll();
            FlightScheduled last = path.get(path.size() - 1);

            if (last.getToAirport().equals(to)) {
                out.add(path);          // reached destination — record, don't extend further
                continue;
            }
            if (path.size() >= props.getMaxLegs()) {
                continue;               // cannot add another leg
            }

            // airports already on this path (origin + each arrival) — no cycles / no revisits
            Set<String> visited = new HashSet<>();
            visited.add(from);
            for (FlightScheduled l : path) {
                visited.add(l.getToAirport());
            }

            for (FlightScheduled next : adjacency.getOrDefault(last.getToAirport(), List.of())) {
                long gap = Duration.between(last.getArrivalTime(), next.getDepartureTime()).toMinutes();
                if (gap < props.getMinConnectionMinutes() || gap > props.getMaxLayoverMinutes()) {
                    continue;           // too tight to connect, or too long a layover
                }
                if (visited.contains(next.getToAirport())) {
                    continue;           // would revisit an airport already in the path
                }
                List<FlightScheduled> extended = new ArrayList<>(path);
                extended.add(next);
                queue.add(extended);
            }
        }
        return out;
    }

    private Itinerary toItinerary(List<FlightScheduled> path,
                                  Map<Long, Integer> seatCounts,
                                  Map<Long, BigDecimal> prices) {
        List<Leg> legs = new ArrayList<>(path.size());
        BigDecimal total = BigDecimal.ZERO;
        int minSeats = Integer.MAX_VALUE;
        for (FlightScheduled fs : path) {
            BigDecimal price = prices.getOrDefault(fs.getId(), BigDecimal.ZERO);
            int seats = seatCounts.getOrDefault(fs.getId(), 0);
            total = total.add(price);
            minSeats = Math.min(minSeats, seats);
            legs.add(new Leg(
                    fs.getId(),
                    fs.getFlight().getId(),
                    fs.getFlight().getFlightName(),
                    fs.getFromAirport(),
                    fs.getToAirport(),
                    fs.getDepartureTime(),
                    fs.getArrivalTime(),
                    fs.durationMinutes(),
                    price,
                    seats
            ));
        }
        Leg first = legs.get(0);
        Leg lastLeg = legs.get(legs.size() - 1);
        long totalDuration = Duration.between(first.departureTime(), lastLeg.arrivalTime()).toMinutes();
        return new Itinerary(
                legs.size() == 1,
                legs.size() - 1,
                first.fromAirport(),
                lastLeg.toAirport(),
                first.departureTime(),
                lastLeg.arrivalTime(),
                totalDuration,
                total,
                minSeats == Integer.MAX_VALUE ? 0 : minSeats,
                legs
        );
    }
}
