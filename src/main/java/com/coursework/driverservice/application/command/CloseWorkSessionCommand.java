package com.coursework.driverservice.application.command;

public record CloseWorkSessionCommand(
        Long sessionId,
        Long driverId
) {
}