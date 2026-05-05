package com.coursework.driverservice.infrastructure.web.dto;

import java.time.Instant;

public record RouteDto(
        Long id,
        Long tripId,
        String encodedPolyline,
        int totalDistanceM,
        int totalDurationS,
        String provider,
        Instant calculatedAt
) {
}
