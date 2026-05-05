package com.coursework.driverservice.infrastructure.persistence.spec;

import com.coursework.driverservice.infrastructure.persistence.entity.TripEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.TripStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class TripSpecifications {

    private TripSpecifications() {
    }

    public static Specification<TripEntity> hasStatus(TripStatus status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<TripEntity> hasDriver(Long driverId) {
        return (root, query, cb) ->
                driverId == null ? cb.conjunction() : cb.equal(root.get("driver").get("id"), driverId);
    }

    public static Specification<TripEntity> hasVehicle(Long vehicleId) {
        return (root, query, cb) ->
                vehicleId == null ? cb.conjunction() : cb.equal(root.get("vehicle").get("id"), vehicleId);
    }

    public static Specification<TripEntity> plannedBetween(Instant from, Instant to) {
        return (root, query, cb) -> {
            if (from != null && to != null) {
                return cb.between(root.get("plannedStartAt"), from, to);
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("plannedStartAt"), from);
            }
            if (to != null) {
                return cb.lessThanOrEqualTo(root.get("plannedStartAt"), to);
            }
            return cb.conjunction();
        };
    }
}
