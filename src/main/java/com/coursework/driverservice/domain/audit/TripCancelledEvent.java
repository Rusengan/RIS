package com.coursework.driverservice.domain.audit;

import java.util.Map;

public record TripCancelledEvent(Long userId, Long entityId, Map<String, Object> payload) implements AuditEvent {
}
