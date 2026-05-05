package com.coursework.driverservice.application.command;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AppendRoutePointsCommand(
        Long tripId,
        Long dispatcherId,
        @NotEmpty List<CreateRoutePointCommand> points
) {
}
