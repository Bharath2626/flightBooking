package com.flight.booking.service;

import com.flight.booking.domain.entity.SearchLog;
import com.flight.booking.repository.SearchLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** Persists a row for every search. Isolated so a logging failure never breaks search. */
@Service
public class SearchLogService {

    private static final Logger log = LoggerFactory.getLogger(SearchLogService.class);
    private final SearchLogRepository repository;

    public SearchLogService(SearchLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String from, String to, LocalDate date, int noOfSeats, Long userId,
                       int resultCount, int pageNumber, String sortKey, boolean servedFromCache) {
        try {
            SearchLog entry = new SearchLog();
            entry.setFromAirport(from);
            entry.setToAirport(to);
            entry.setTravelDate(date);
            entry.setNoOfSeats(noOfSeats);
            entry.setUserId(userId);
            entry.setResultCount(resultCount);
            entry.setPageNumber(pageNumber);
            entry.setSortKey(sortKey);
            entry.setServedFromCache(servedFromCache);
            repository.save(entry);
        } catch (RuntimeException ex) {
            log.warn("Failed to persist search log: {}", ex.getMessage());
        }
    }
}
