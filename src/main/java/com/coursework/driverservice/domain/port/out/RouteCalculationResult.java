package com.coursework.driverservice.domain.port.out;

import java.time.Instant;

public record RouteCalculationResult(
        String encodedPolyline,
        int distanceMeters,
        int durationSeconds,
        String provider,
        Instant calculatedAt
) {
}
