package com.coursework.driverservice.infrastructure.web.dto;

import com.coursework.driverservice.infrastructure.persistence.entity.RoutePointType;

import java.math.BigDecimal;
import java.time.Instant;

public record RoutePointDto(
        Long id,
        Long tripId,
        Short sequenceNo,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        RoutePointType pointType,
        Instant arrivedAt
) {
}
