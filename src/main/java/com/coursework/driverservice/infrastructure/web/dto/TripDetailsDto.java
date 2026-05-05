package com.coursework.driverservice.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Детали рейса с маршрутом и точками")
public record TripDetailsDto(
        TripDto trip,
        RouteDto route,
        List<RoutePointDto> routePoints
) {
}
