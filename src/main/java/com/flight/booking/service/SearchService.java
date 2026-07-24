package com.flight.booking.service;

import com.flight.booking.dto.PageResponse;
import com.flight.booking.dto.SortKey;
import com.flight.booking.dto.SortType;
import com.flight.booking.model.Itinerary;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Serves flight searches entirely from the precomputed {@link ItineraryCache}:
 * filter by seats, prefer direct flights (fall back to connecting when none qualify),
 * sort, paginate, and log the search.
 */
@Service
public class SearchService {

    private final ItineraryCache cache;
    private final SearchLogService searchLogService;

    public SearchService(ItineraryCache cache, SearchLogService searchLogService) {
        this.cache = cache;
        this.searchLogService = searchLogService;
    }

    public PageResponse<Itinerary> search(String from, String to, LocalDate date, int noOfSeats,
                                          SortKey sortKey, SortType sortType,
                                          int pageNumber, int pageSize, Long userId) {

        if (noOfSeats < 1) noOfSeats = 1;
        if (pageNumber < 1) pageNumber = 1;
        if (pageSize < 1) pageSize = 10;
        if (pageSize > 100) pageSize = 100;
        SortKey key = sortKey == null ? SortKey.PRICE : sortKey;
        SortType type = sortType == null ? SortType.ASC : sortType;

        boolean wasCached = cache.isCached(from, to, date);
        List<Itinerary> all = cache.get(from, to, date);

        final int seats = noOfSeats;
        List<Itinerary> bookable = all.stream()
                .filter(i -> i.availableSeats() >= seats)
                .toList();

        // Requirement: show connecting flights only when no direct option is available.
        List<Itinerary> directs = bookable.stream().filter(Itinerary::direct).toList();
        List<Itinerary> candidates = directs.isEmpty() ? bookable : directs;

        List<Itinerary> sorted = candidates.stream()
                .sorted(comparator(key, type))
                .toList();

        PageResponse<Itinerary> page = PageResponse.of(sorted, pageNumber, pageSize, wasCached);

        searchLogService.record(from, to, date, seats, userId,
                sorted.size(), pageNumber, key.name(), wasCached);

        return page;
    }

    private Comparator<Itinerary> comparator(SortKey key, SortType type) {
        Comparator<Itinerary> base = switch (key) {
            case PRICE -> Comparator.comparing(Itinerary::totalPrice)
                    .thenComparing(Itinerary::totalDurationMinutes);
            case DURATION -> Comparator.comparingLong(Itinerary::totalDurationMinutes)
                    .thenComparing(Itinerary::totalPrice);
            case DEPARTURE -> Comparator.comparing(Itinerary::departureTime)
                    .thenComparing(Itinerary::totalPrice);
        };
        return type == SortType.DESC ? base.reversed() : base;
    }
}
