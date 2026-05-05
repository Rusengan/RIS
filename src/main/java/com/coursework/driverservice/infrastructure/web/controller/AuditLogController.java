package com.coursework.driverservice.infrastructure.web.controller;

import com.coursework.driverservice.application.query.AuditLogQueryService;
import com.coursework.driverservice.infrastructure.web.dto.AuditLogDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit logs", description = "Журнал аудита")
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Список записей аудита")
    @ApiResponse(responseCode = "200", description = "Страница записей")
    public Page<AuditLogDto> list(
            @Parameter(description = "Фильтр по пользователю")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "Тип сущности")
            @RequestParam(required = false) String entityType,
            @Parameter(description = "Начало периода")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "Конец периода")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            Pageable pageable
    ) {
        return auditLogQueryService.search(userId, entityType, from, to, pageable);
    }
}
