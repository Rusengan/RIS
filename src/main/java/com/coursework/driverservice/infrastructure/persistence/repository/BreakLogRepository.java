package com.coursework.driverservice.infrastructure.persistence.repository;

import com.coursework.driverservice.infrastructure.persistence.entity.BreakLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface BreakLogRepository extends JpaRepository<BreakLogEntity, Long> {
    
    Optional<BreakLogEntity> findFirstByWorkSessionIdAndEndedAtIsNull(Long sessionId);
    
    @Query("SELECT COALESCE(SUM(bl.durationMinutes), 0) FROM break_logs bl " +
           "JOIN bl.workSession ws " +
           "WHERE ws.driver.id = :driverId AND ws.startedAt >= :from AND ws.startedAt <= :to " +
           "AND bl.durationMinutes IS NOT NULL")
    Integer sumBreakDurationByDriverIdAndDateRange(@Param("driverId") Long driverId, @Param("from") Instant from, @Param("to") Instant to);
}