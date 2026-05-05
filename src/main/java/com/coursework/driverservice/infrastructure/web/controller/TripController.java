package com.coursework.driverservice.infrastructure.web.controller;

import com.coursework.driverservice.application.command.AcceptTripCommand;
import com.coursework.driverservice.application.command.AppendRoutePointsCommand;
import com.coursework.driverservice.application.command.CalculateRouteCommand;
import com.coursework.driverservice.application.command.CancelTripCommand;
import com.coursework.driverservice.application.command.CompleteTripCommand;
import com.coursework.driverservice.application.command.CreateTripCommand;
import com.coursework.driverservice.application.handler.AcceptTripCommandHandler;
import com.coursework.driverservice.application.handler.AppendRoutePointsCommandHandler;
import com.coursework.driverservice.application.handler.CalculateRouteCommandHandler;
import com.coursework.driverservice.application.handler.CancelTripCommandHandler;
import com.coursework.driverservice.application.handler.CompleteTripCommandHandler;
import com.coursework.driverservice.application.handler.CreateTripCommandHandler;
import com.coursework.driverservice.application.query.TripQueryService;
import com.coursework.driverservice.infrastructure.persistence.entity.TripStatus;
import com.coursework.driverservice.infrastructure.persistence.repository.TripRepository;
import com.coursework.driverservice.infrastructure.web.dto.CancelTripRequest;
import com.coursework.driverservice.infrastructure.web.dto.RouteDto;
import com.coursework.driverservice.infrastructure.web.dto.RoutePointDto;
import com.coursework.driverservice.infrastructure.web.dto.TripDetailsDto;
import com.coursework.driverservice.infrastructure.web.dto.TripDto;
import com.coursework.driverservice.infrastructure.web.exception.NotFoundException;
import com.coursework.driverservice.infrastructure.web.mapper.TripDetailsAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
@Tag(name = "Trips", description = "Создание и жизненный цикл рейсов")
public class TripController {

    private final CreateTripCommandHandler createTripCommandHandler;
    private final AcceptTripCommandHandler acceptTripCommandHandler;
    private final CompleteTripCommandHandler completeTripCommandHandler;
    private final CancelTripCommandHandler cancelTripCommandHandler;
    private final CalculateRouteCommandHandler calculateRouteCommandHandler;
    private final AppendRoutePointsCommandHandler appendRoutePointsCommandHandler;
    private final TripQueryService tripQueryService;
    private final TripRepository tripRepository;
    private final TripDetailsAssembler tripDetailsAssembler;

    private static boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));
    }

    private static Long principalId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    @Operation(summary = "Создать рейс", description = "DISPATCHER или ADMIN; диспетчер из токена или явно для ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Рейс создан",
                    content = @Content(schema = @Schema(implementation = TripDto.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "409", description = "Бизнес-правило нарушено"),
            @ApiResponse(responseCode = "403", description = "Нет прав")
    })
    public ResponseEntity<TripDto> create(@Valid @RequestBody CreateTripCommand command) {
        Long dispatcherId = principalId();
        if (!isAdmin() && !dispatcherId.equals(command.dispatcherId())) {
            throw new AccessDeniedException("Dispatcher mismatch");
        }
        CreateTripCommand effective = new CreateTripCommand(
                command.driverId(),
                command.vehicleId(),
                isAdmin() ? command.dispatcherId() : dispatcherId,
                command.plannedStartAt(),
                command.points()
        );
        TripDto created = createTripCommandHandler.handle(effective);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/trips/" + created.id()))
                .body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    @Operation(summary = "Список рейсов (диспетчер)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Страница рейсов"),
            @ApiResponse(responseCode = "403", description = "Нет прав")
    })
    public Page<TripDto> list(
            @Parameter(description = "Статус") @RequestParam(required = false) TripStatus status,
            @Parameter(description = "Водитель") @RequestParam(required = false) Long driverId,
            @Parameter(description = "ТС") @RequestParam(required = false) Long vehicleId,
            @Parameter(description = "План с") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "План по") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            Pageable pageable
    ) {
        return tripQueryService.search(status, driverId, vehicleId, from, to, pageable);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Мои рейсы (водитель)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Страница рейсов водителя")
    })
    public Page<TripDto> mine(
            @RequestParam(required = false) TripStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            Pageable pageable
    ) {
        Long driverId = principalId();
        return tripQueryService.searchForDriver(driverId, status, from, to, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Детали рейса")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Детали",
                    content = @Content(schema = @Schema(implementation = TripDetailsDto.class))),
            @ApiResponse(responseCode = "404", description = "Не найдено"),
            @ApiResponse(responseCode = "403", description = "Нет доступа")
    })
    public TripDetailsDto getOne(@Parameter(description = "ID рейса") @PathVariable Long id) {
        Long userId = principalId();
        Set<String> roles = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        var trip = tripRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("Trip not found: " + id));

        boolean isDriver = roles.contains("ROLE_DRIVER") && trip.getDriver().getId().equals(userId);
        boolean isDispatcher = roles.contains("ROLE_DISPATCHER") || roles.contains("ROLE_ADMIN");
        if (!isDriver && !isDispatcher) {
            throw new AccessDeniedException("Cannot view trip");
        }

        return tripDetailsAssembler.toDetailsDto(trip);
    }

    @PostMapping("/{id}/route-points")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    @Operation(summary = "Добавить точки маршрута")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Созданные точки"),
            @ApiResponse(responseCode = "409", description = "Конфликт")
    })
    public List<RoutePointDto> appendRoutePoints(
            @Parameter(description = "ID рейса") @PathVariable Long id,
            @Valid @RequestBody List<com.coursework.driverservice.application.command.CreateRoutePointCommand> points
    ) {
        Long dispatcherId = principalId();
        return appendRoutePointsCommandHandler.handle(new AppendRoutePointsCommand(id, dispatcherId, points));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Принять рейс")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Рейс принят",
                    content = @Content(schema = @Schema(implementation = TripDto.class))),
            @ApiResponse(responseCode = "409", description = "Нет открытой смены и т.п.")
    })
    public TripDto accept(@Parameter(description = "ID рейса") @PathVariable Long id) {
        Long driverId = principalId();
        return acceptTripCommandHandler.handle(new AcceptTripCommand(id, driverId));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Завершить рейс")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Завершён",
                    content = @Content(schema = @Schema(implementation = TripDto.class))),
            @ApiResponse(responseCode = "409", description = "Точки не отмечены и т.п.")
    })
    public TripDto complete(@Parameter(description = "ID рейса") @PathVariable Long id) {
        Long driverId = principalId();
        return completeTripCommandHandler.handle(new CompleteTripCommand(id, driverId));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    @Operation(summary = "Отменить рейс")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Отменён"),
            @ApiResponse(responseCode = "409", description = "Нельзя отменить")
    })
    public TripDto cancel(
            @Parameter(description = "ID рейса") @PathVariable Long id,
            @Valid @RequestBody CancelTripRequest body
    ) {
        Long dispatcherId = principalId();
        return cancelTripCommandHandler.handle(new CancelTripCommand(id, dispatcherId, body.reason()));
    }

    @PostMapping("/{id}/route/calculate")
    @PreAuthorize("hasAnyRole('DRIVER', 'DISPATCHER', 'ADMIN')")
    @Operation(summary = "Рассчитать маршрут")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Маршрут",
                    content = @Content(schema = @Schema(implementation = RouteDto.class))),
            @ApiResponse(responseCode = "502", description = "Внешний сервис маршрутов"),
            @ApiResponse(responseCode = "409", description = "Нет точек ORIGIN/DESTINATION")
    })
    public RouteDto calculateRoute(@Parameter(description = "ID рейса") @PathVariable Long id) {
        Long userId = principalId();
        var trip = tripRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("Trip not found: " + id));

        Set<String> roles = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        boolean isDriver = roles.contains("ROLE_DRIVER") && trip.getDriver().getId().equals(userId);
        boolean isStaff = roles.contains("ROLE_DISPATCHER") || roles.contains("ROLE_ADMIN");
        if (!isDriver && !isStaff) {
            throw new AccessDeniedException("Cannot calculate route for this trip");
        }

        return calculateRouteCommandHandler.handle(new CalculateRouteCommand(id));
    }
}
