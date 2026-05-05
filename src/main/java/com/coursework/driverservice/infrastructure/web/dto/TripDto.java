package com.coursework.driverservice.infrastructure.web.dto;

import com.coursework.driverservice.infrastructure.persistence.entity.TripStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Рейс (кратко)")
public record TripDto(
        Long id,
        Long driverId,
        String driverFullName,
        Long vehicleId,
        String vehiclePlate,
        Long dispatcherId,
        Long workSessionId,
        TripStatus status,
        Instant plannedStartAt,
        Instant actualStartAt,
        Instant actualEndAt,
        Integer totalDistanceM,
        Integer totalDurationS,
        String cancelReason,
        Instant createdAt,
        Instant updatedAt
) {
}
