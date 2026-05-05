package com.coursework.driverservice.application.command;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record CreateTripCommand(
        @NotNull Long driverId,
        @NotNull Long vehicleId,
        @NotNull Long dispatcherId,
        @NotNull @Future Instant plannedStartAt,
        @NotEmpty @Size(min = 2) List<CreateRoutePointCommand> points
) {
}
