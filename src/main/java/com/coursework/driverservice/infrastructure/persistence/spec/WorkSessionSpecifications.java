package com.coursework.driverservice.infrastructure.persistence.spec;

import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class WorkSessionSpecifications {

    private WorkSessionSpecifications() {
    }

    public static Specification<WorkSessionEntity> hasDriverId(Long driverId) {
        return (root, query, cb) -> driverId == null
                ? cb.conjunction()
                : cb.equal(root.get("driver").get("id"), driverId);
    }

    public static Specification<WorkSessionEntity> startedAfter(Instant from) {
        return (root, query, cb) -> from == null
                ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("startedAt"), from);
    }

    public static Specification<WorkSessionEntity> startedBefore(Instant to) {
        return (root, query, cb) -> to == null
                ? cb.conjunction()
                : cb.lessThanOrEqualTo(root.get("startedAt"), to);
    }

    public static Specification<WorkSessionEntity> hasStatus(WorkSessionStatus status) {
        return (root, query, cb) -> status == null
                ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }
}