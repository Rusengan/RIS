package com.coursework.driverservice.domain.audit;

import java.util.Map;

public record TripCompletedEvent(Long userId, Long entityId, Map<String, Object> payload) implements AuditEvent {
}
