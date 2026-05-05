package com.coursework.driverservice.infrastructure.persistence.spec;

import com.coursework.driverservice.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    public static Specification<AuditLogEntity> byUser(Long userId) {
        return (root, query, cb) ->
                userId == null ? cb.conjunction() : cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<AuditLogEntity> byEntityType(String entityType) {
        return (root, query, cb) ->
                entityType == null || entityType.isBlank()
                        ? cb.conjunction()
                        : cb.equal(root.get("entityType"), entityType);
    }

    public static Specification<AuditLogEntity> createdBetween(Instant from, Instant to) {
        return (root, query, cb) -> {
            if (from != null && to != null) {
                return cb.between(root.get("createdAt"), from, to);
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            }
            if (to != null) {
                return cb.lessThanOrEqualTo(root.get("createdAt"), to);
            }
            return cb.conjunction();
        };
    }
}
