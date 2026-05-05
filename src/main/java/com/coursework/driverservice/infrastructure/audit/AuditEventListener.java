package com.coursework.driverservice.infrastructure.audit;

import com.coursework.driverservice.domain.audit.TripCancelledEvent;
import com.coursework.driverservice.domain.audit.TripCompletedEvent;
import com.coursework.driverservice.domain.audit.TripCreatedEvent;
import com.coursework.driverservice.domain.audit.UserCreatedEvent;
import com.coursework.driverservice.domain.audit.WorkSessionClosedEvent;
import com.coursework.driverservice.domain.audit.WorkSessionStartedEvent;
import com.coursework.driverservice.infrastructure.persistence.entity.AuditLogEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.UserEntity;
import com.coursework.driverservice.infrastructure.persistence.repository.AuditLogRepository;
import com.coursework.driverservice.infrastructure.persistence.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onTripCreated(TripCreatedEvent event) {
        persist(event.userId(), AuditActions.TRIP_CREATED, AuditEntityTypes.TRIP, event.entityId(), event.payload());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onTripCompleted(TripCompletedEvent event) {
        persist(event.userId(), AuditActions.TRIP_COMPLETED, AuditEntityTypes.TRIP, event.entityId(), event.payload());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onTripCancelled(TripCancelledEvent event) {
        persist(event.userId(), AuditActions.TRIP_CANCELLED, AuditEntityTypes.TRIP, event.entityId(), event.payload());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onWorkSessionStarted(WorkSessionStartedEvent event) {
        persist(event.userId(), AuditActions.WORK_SESSION_STARTED, AuditEntityTypes.WORK_SESSION, event.entityId(), event.payload());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onWorkSessionClosed(WorkSessionClosedEvent event) {
        persist(event.userId(), AuditActions.WORK_SESSION_CLOSED, AuditEntityTypes.WORK_SESSION, event.entityId(), event.payload());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onUserCreated(UserCreatedEvent event) {
        persist(event.userId(), AuditActions.USER_CREATED, AuditEntityTypes.USER, event.entityId(), event.payload());
    }

    private void persist(Long userId, String action, String entityType, Long entityId, Map<String, Object> payload) {
        try {
            String json = payload == null || payload.isEmpty() ? null : objectMapper.writeValueAsString(payload);
            UserEntity user = null;
            if (userId != null) {
                user = userRepository.getReferenceById(userId);
            }
            AuditLogEntity entity = AuditLogEntity.builder()
                    .user(user)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .payloadJson(json)
                    .createdAt(Instant.now())
                    .build();
            auditLogRepository.save(entity);
        } catch (Exception e) {
            throw new IllegalStateException("Audit persist failed", e);
        }
    }
}
