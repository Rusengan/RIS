package com.coursework.driverservice.infrastructure.web.dto;

import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Рабочая смена")
public record WorkSessionDto(
        Long id,
        Long driverId,
        Instant startedAt,
        Instant endedAt,
        Integer totalMinutes,
        WorkSessionStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<BreakLogDto> breaks
) {
}