package com.coursework.driverservice.infrastructure.persistence.repository;

import com.coursework.driverservice.infrastructure.persistence.entity.RoleCode;
import com.coursework.driverservice.infrastructure.persistence.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByCode(RoleCode code);
}
