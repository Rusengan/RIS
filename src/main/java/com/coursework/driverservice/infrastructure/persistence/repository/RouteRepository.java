package com.coursework.driverservice.infrastructure.persistence.repository;

import com.coursework.driverservice.infrastructure.persistence.entity.RouteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RouteRepository extends JpaRepository<RouteEntity, Long> {

    Optional<RouteEntity> findByTripId(Long tripId);
}
