package com.coursework.driverservice.domain.audit;

import java.util.Map;

public record WorkSessionClosedEvent(Long userId, Long entityId, Map<String, Object> payload) implements AuditEvent {
}
