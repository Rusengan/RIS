package com.coursework.driverservice.infrastructure.persistence.repository;

import com.coursework.driverservice.infrastructure.persistence.entity.RoutePointEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoutePointRepository extends JpaRepository<RoutePointEntity, Long> {

    List<RoutePointEntity> findByTripIdOrderBySequenceNoAsc(Long tripId);
}
