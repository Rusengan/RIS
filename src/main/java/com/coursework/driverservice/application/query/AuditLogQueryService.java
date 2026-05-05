package com.coursework.driverservice.application.query;

import com.coursework.driverservice.infrastructure.persistence.entity.AuditLogEntity;
import com.coursework.driverservice.infrastructure.persistence.repository.AuditLogRepository;
import com.coursework.driverservice.infrastructure.persistence.spec.AuditLogSpecifications;
import com.coursework.driverservice.infrastructure.web.dto.AuditLogDto;
import com.coursework.driverservice.infrastructure.web.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    @Transactional(readOnly = true)
    public Page<AuditLogDto> search(Long userId, String entityType, Instant from, Instant to, Pageable pageable) {
        Specification<AuditLogEntity> spec = Specification
                .where(AuditLogSpecifications.byUser(userId))
                .and(AuditLogSpecifications.byEntityType(entityType))
                .and(AuditLogSpecifications.createdBetween(from, to));
        return auditLogRepository.findAll(spec, pageable).map(auditLogMapper::toDto);
    }
}
