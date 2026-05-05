package com.coursework.driverservice.domain.port.out;

import java.util.List;

public record RouteCalculationRequest(
        GeoPoint origin,
        GeoPoint destination,
        List<GeoPoint> waypoints
) {
}
