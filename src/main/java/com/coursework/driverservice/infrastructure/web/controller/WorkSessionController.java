package com.coursework.driverservice.infrastructure.web.controller;

import com.coursework.driverservice.application.command.CloseWorkSessionCommand;
import com.coursework.driverservice.application.command.StartBreakCommand;
import com.coursework.driverservice.application.command.StartWorkSessionCommand;
import com.coursework.driverservice.application.handler.CloseWorkSessionCommandHandler;
import com.coursework.driverservice.application.handler.StartBreakCommandHandler;
import com.coursework.driverservice.application.handler.StartWorkSessionCommandHandler;
import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionStatus;
import com.coursework.driverservice.infrastructure.persistence.repository.WorkSessionRepository;
import com.coursework.driverservice.infrastructure.persistence.spec.WorkSessionSpecifications;
import com.coursework.driverservice.infrastructure.web.dto.BreakLogDto;
import com.coursework.driverservice.infrastructure.web.dto.StartBreakRequest;
import com.coursework.driverservice.infrastructure.web.dto.WorkSessionDto;
import com.coursework.driverservice.infrastructure.web.mapper.WorkSessionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/work-sessions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Work sessions", description = "Рабочие смены и перерывы")
public class WorkSessionController {

    private final WorkSessionRepository workSessionRepository;
    private final WorkSessionMapper workSessionMapper;
    private final StartWorkSessionCommandHandler startWorkSessionCommandHandler;
    private final CloseWorkSessionCommandHandler closeWorkSessionCommandHandler;
    private final StartBreakCommandHandler startBreakCommandHandler;

    @PostMapping("/start")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Начать смену")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Смена открыта",
                    content = @Content(schema = @Schema(implementation = WorkSessionDto.class))),
            @ApiResponse(responseCode = "409", description = "Смена уже открыта")
    })
    public ResponseEntity<WorkSessionDto> start() {
        Long driverId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        StartWorkSessionCommand command = new StartWorkSessionCommand(driverId);
        WorkSessionDto created = startWorkSessionCommandHandler.handle(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/work-sessions/" + created.id()))
                .body(created);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Закрыть смену")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Смена закрыта"),
            @ApiResponse(responseCode = "409", description = "Активный рейс и т.п.")
    })
    public WorkSessionDto close(@Parameter(description = "ID смены") @PathVariable Long id) {
        Long driverId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        CloseWorkSessionCommand command = new CloseWorkSessionCommand(id, driverId);
        return closeWorkSessionCommandHandler.handle(command);
    }

    @GetMapping("/current")
    @PreAuthorize("hasRole('DRIVER')")
    @Transactional(readOnly = true)
    @Operation(summary = "Текущая открытая смена")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Есть открытая смена"),
            @ApiResponse(responseCode = "204", description = "Нет открытой смены")
    })
    public ResponseEntity<WorkSessionDto> current() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Current endpoint called by user: {}, authorities: {}",
                auth != null ? auth.getPrincipal() : "null",
                auth != null ? auth.getAuthorities() : "null");
        Long driverId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // JOIN FETCH version preloads breaks eagerly so the mapper can safely access
        // the lazy collection. The @Transactional(readOnly = true) keeps the session
        // open for any other lazy access just in case.
        return workSessionRepository.findCurrentByDriverIdAndStatus(driverId, WorkSessionStatus.OPEN)
                .map(workSessionMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    @Operation(summary = "Список смен")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Страница смен")
    })
    public Page<WorkSessionDto> list(
            @Parameter(description = "Водитель") @RequestParam(required = false) Long driverId,
            @Parameter(description = "С") @RequestParam(required = false) Instant from,
            @Parameter(description = "По") @RequestParam(required = false) Instant to,
            @Parameter(description = "Статус") @RequestParam(required = false) WorkSessionStatus status,
            Pageable pageable
    ) {
        Specification<WorkSessionEntity> spec = Specification
                .where(WorkSessionSpecifications.hasDriverId(driverId))
                .and(WorkSessionSpecifications.startedAfter(from))
                .and(WorkSessionSpecifications.startedBefore(to))
                .and(WorkSessionSpecifications.hasStatus(status));

        return workSessionRepository.findAll(spec, pageable)
                .map(workSessionMapper::toDto);
    }

    @PostMapping("/{id}/breaks/start")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Начать перерыв")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Перерыв начат"),
            @ApiResponse(responseCode = "409", description = "Уже есть активный перерыв")
    })
    public BreakLogDto startBreak(
            @Parameter(description = "ID смены") @PathVariable Long id,
            @Valid @RequestBody StartBreakRequest request
    ) {
        Long driverId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        StartBreakCommand command = new StartBreakCommand(id, driverId, request.breakType());
        return startBreakCommandHandler.handle(command);
    }
}
