package com.coursework.driverservice.application.command;

public record CancelTripCommand(Long tripId, Long dispatcherId, String reason) {
}
