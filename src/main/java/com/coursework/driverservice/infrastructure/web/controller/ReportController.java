package com.coursework.driverservice.infrastructure.web.controller;

import com.coursework.driverservice.application.service.TimesheetReportService;
import com.coursework.driverservice.infrastructure.web.dto.TimesheetReportDto;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final TimesheetReportService timesheetReportService;

    @GetMapping("/timesheet")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public TimesheetReportDto getTimesheet(
            @RequestParam Long driverId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return timesheetReportService.generate(driverId, from, to);
    }
}