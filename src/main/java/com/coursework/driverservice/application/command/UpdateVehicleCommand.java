package com.coursework.driverservice.application.command;

import com.coursework.driverservice.infrastructure.persistence.entity.VehicleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateVehicleCommand(
        @NotBlank String plateNumber,
        @NotBlank String brand,
        @NotBlank String model,
        Integer capacityKg,
        @NotNull VehicleStatus status
) {
}
