package com.coursework.driverservice.infrastructure.persistence.repository;

import com.coursework.driverservice.infrastructure.persistence.entity.VehicleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface VehicleRepository extends JpaRepository<VehicleEntity, Long>,
        JpaSpecificationExecutor<VehicleEntity> {

    Optional<VehicleEntity> findByPlateNumber(String plateNumber);

    boolean existsByPlateNumberAndIdNot(String plateNumber, Long id);
}
