package com.coursework.driverservice.infrastructure.persistence.repository;

import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkSessionRepository extends JpaRepository<WorkSessionEntity, Long>, JpaSpecificationExecutor<WorkSessionEntity> {
    
    Optional<WorkSessionEntity> findByDriverIdAndStatus(Long driverId, WorkSessionStatus status);

    /**
     * Returns the current work session for a driver by status with breaks eagerly loaded.
     * Used by GET /api/v1/work-sessions/current to avoid LazyInitializationException
     * in the mapper which runs outside of a transaction in the controller.
     */
    @Query("""
            SELECT DISTINCT ws FROM work_sessions ws
            LEFT JOIN FETCH ws.breaks
            WHERE ws.driver.id = :driverId AND ws.status = :status
            """)
    Optional<WorkSessionEntity> findCurrentByDriverIdAndStatus(
            @Param("driverId") Long driverId,
            @Param("status") WorkSessionStatus status);
    
    @Query("""
            SELECT DISTINCT ws FROM work_sessions ws
            LEFT JOIN FETCH ws.breaks
            WHERE ws.driver.id = :driverId AND ws.startedAt >= :from AND ws.startedAt <= :to
            ORDER BY ws.startedAt DESC
            """)
    List<WorkSessionEntity> findByDriverIdAndStartedAtBetween(@Param("driverId") Long driverId, @Param("from") Instant from, @Param("to") Instant to);
    
    @Query("SELECT COALESCE(SUM(ws.totalMinutes), 0) FROM work_sessions ws WHERE ws.driver.id = :driverId AND ws.startedAt >= :from AND ws.startedAt <= :to AND ws.totalMinutes IS NOT NULL")
    Integer sumTotalMinutesByDriverIdAndDateRange(@Param("driverId") Long driverId, @Param("from") Instant from, @Param("to") Instant to);
    
    @Query("SELECT COUNT(ws) FROM work_sessions ws WHERE ws.driver.id = :driverId AND ws.startedAt >= :from AND ws.startedAt <= :to")
    Long countByDriverIdAndDateRange(@Param("driverId") Long driverId, @Param("from") Instant from, @Param("to") Instant to);
}