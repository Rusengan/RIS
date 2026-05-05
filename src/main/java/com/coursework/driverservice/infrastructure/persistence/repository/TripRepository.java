package com.coursework.driverservice.infrastructure.persistence.repository;

import com.coursework.driverservice.infrastructure.persistence.entity.TripEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.TripStatus;
import com.coursework.driverservice.infrastructure.persistence.entity.VehicleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<TripEntity, Long>, JpaSpecificationExecutor<TripEntity> {

    @Query("""
            SELECT DISTINCT t FROM trips t
            LEFT JOIN FETCH t.driver
            LEFT JOIN FETCH t.vehicle
            LEFT JOIN FETCH t.dispatcher
            LEFT JOIN FETCH t.workSession
            LEFT JOIN FETCH t.route
            LEFT JOIN FETCH t.routePoints
            WHERE t.id = :id
            """)
    Optional<TripEntity> findDetailById(@Param("id") Long id);

    boolean existsByVehicleAndStatusIn(VehicleEntity vehicle, Collection<TripStatus> statuses);

    boolean existsByDriverIdAndStatus(Long driverId, TripStatus status);

    List<TripEntity> findByWorkSessionIdAndStatus(Long workSessionId, TripStatus status);
}
