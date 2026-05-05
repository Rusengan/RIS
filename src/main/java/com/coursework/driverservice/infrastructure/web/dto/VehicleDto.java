package com.coursework.driverservice.infrastructure.web.dto;

import com.coursework.driverservice.infrastructure.persistence.entity.VehicleStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Транспортное средство")
public record VehicleDto(
        Long id,
        String plateNumber,
        String brand,
        String model,
        Integer capacityKg,
        VehicleStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
