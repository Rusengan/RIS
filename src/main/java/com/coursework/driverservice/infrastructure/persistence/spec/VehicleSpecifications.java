package com.coursework.driverservice.infrastructure.persistence.spec;

import com.coursework.driverservice.infrastructure.persistence.entity.VehicleEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.VehicleStatus;
import org.springframework.data.jpa.domain.Specification;

public final class VehicleSpecifications {

    private VehicleSpecifications() {
    }

    public static Specification<VehicleEntity> byStatus(VehicleStatus status) {
        return (root, query, cb) -> status == null
                ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }
}
