package com.coursework.driverservice.domain.audit;

import java.util.Map;

public sealed interface AuditEvent permits
        TripCreatedEvent,
        TripCompletedEvent,
        TripCancelledEvent,
        WorkSessionStartedEvent,
        WorkSessionClosedEvent,
        UserCreatedEvent {
}
