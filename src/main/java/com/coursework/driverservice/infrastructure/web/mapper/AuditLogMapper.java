package com.coursework.driverservice.infrastructure.web.mapper;

import com.coursework.driverservice.infrastructure.persistence.entity.AuditLogEntity;
import com.coursework.driverservice.infrastructure.web.dto.AuditLogDto;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

    public AuditLogDto toDto(AuditLogEntity entity) {
        Long userId = entity.getUser() != null ? entity.getUser().getId() : null;
        String fullName = entity.getUser() != null ? entity.getUser().getFullName() : null;
        return new AuditLogDto(
                entity.getId(),
                userId,
                fullName,
                entity.getAction(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getPayloadJson(),
                entity.getCreatedAt()
        );
    }
}
