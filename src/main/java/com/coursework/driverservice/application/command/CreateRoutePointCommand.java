package com.coursework.driverservice.application.command;

import com.coursework.driverservice.infrastructure.persistence.entity.RoutePointType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateRoutePointCommand(
        @NotNull Short sequenceNo,
        @NotBlank String address,
        @NotNull BigDecimal latitude,
        @NotNull BigDecimal longitude,
        @NotNull RoutePointType type
) {
}
