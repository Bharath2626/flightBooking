package com.flight.booking.dto;

import java.util.List;

/** Generic pagination envelope returned by search. */
public record PageResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean servedFromCache
) {
    public static <T> PageResponse<T> of(List<T> all, int pageNumber, int pageSize, boolean servedFromCache) {
        int total = all.size();
        int totalPages = pageSize <= 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        int from = Math.min((pageNumber - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        List<T> slice = from >= to ? List.of() : List.copyOf(all.subList(from, to));
        return new PageResponse<>(slice, pageNumber, pageSize, total, totalPages,
                pageNumber < totalPages, servedFromCache);
    }
}
