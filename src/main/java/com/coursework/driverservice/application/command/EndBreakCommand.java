package com.coursework.driverservice.application.command;

public record EndBreakCommand(
        Long breakId,
        Long driverId
) {
}