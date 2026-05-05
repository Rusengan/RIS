package com.coursework.driverservice.application.command;

import com.coursework.driverservice.infrastructure.persistence.entity.BreakType;

public record StartBreakCommand(
        Long sessionId,
        Long driverId,
        BreakType breakType
) {
}