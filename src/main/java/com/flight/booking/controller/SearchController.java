package com.flight.booking.controller;

import com.flight.booking.dto.PageResponse;
import com.flight.booking.dto.SortKey;
import com.flight.booking.dto.SortType;
import com.flight.booking.model.Itinerary;
import com.flight.booking.security.AuthPrincipal;
import com.flight.booking.service.SearchService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Search flights. Results come from the precomputed cache; sorting and pagination
     * are applied in-memory. Direct flights are returned when available, otherwise
     * connecting itineraries. Works with or without authentication.
     */
    @GetMapping
    public PageResponse<Itinerary> search(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1") int seats,
            @RequestParam(defaultValue = "PRICE") SortKey sortKey,
            @RequestParam(defaultValue = "ASC") SortType sortType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal AuthPrincipal principal) {

        Long userId = principal == null ? null : principal.userId();
        return searchService.search(from, to, date, seats, sortKey, sortType, page, size, userId);
    }
}
