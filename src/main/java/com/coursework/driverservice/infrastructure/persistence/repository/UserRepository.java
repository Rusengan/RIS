package com.coursework.driverservice.infrastructure.persistence.repository;

import com.coursework.driverservice.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long>, JpaSpecificationExecutor<UserEntity> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByGoogleSubject(String sub);

    @Query("SELECT DISTINCT u FROM users u LEFT JOIN FETCH u.roles WHERE u.id = :id")
    Optional<UserEntity> findByIdWithRoles(@Param("id") Long id);
}
