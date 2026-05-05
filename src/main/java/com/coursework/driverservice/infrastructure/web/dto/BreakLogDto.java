package com.coursework.driverservice.infrastructure.web.dto;

import com.coursework.driverservice.infrastructure.persistence.entity.BreakType;

import java.time.Instant;

public record BreakLogDto(
        Long id,
        Long workSessionId,
        BreakType breakType,
        Instant startedAt,
        Instant endedAt,
        Integer durationMinutes,
        Instant createdAt,
        Instant updatedAt
) {
}