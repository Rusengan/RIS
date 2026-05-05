package com.coursework.driverservice.infrastructure.web.dto;

import java.time.Instant;
import java.util.List;

public record TimesheetReportDto(
        Long driverId,
        String driverFullName,
        Instant from,
        Instant to,
        int totalWorkMinutes,
        int totalBreakMinutes,
        int sessionsCount,
        List<WorkSessionDto> sessions
) {
}