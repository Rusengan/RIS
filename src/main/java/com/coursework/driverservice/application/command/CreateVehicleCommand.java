package com.coursework.driverservice.application.command;

import com.coursework.driverservice.infrastructure.persistence.entity.VehicleStatus;
import jakarta.validation.constraints.NotBlank;

public record CreateVehicleCommand(
        @NotBlank String plateNumber,
        @NotBlank String brand,
        @NotBlank String model,
        Integer capacityKg,
        VehicleStatus status
) {
}
