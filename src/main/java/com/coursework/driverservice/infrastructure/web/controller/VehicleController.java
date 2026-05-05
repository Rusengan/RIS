package com.coursework.driverservice.infrastructure.web.controller;

import com.coursework.driverservice.application.command.CreateVehicleCommand;
import com.coursework.driverservice.application.command.UpdateVehicleCommand;
import com.coursework.driverservice.application.handler.CreateVehicleCommandHandler;
import com.coursework.driverservice.application.handler.DeleteVehicleCommandHandler;
import com.coursework.driverservice.application.handler.UpdateVehicleCommandHandler;
import com.coursework.driverservice.application.query.VehicleQueryService;
import com.coursework.driverservice.infrastructure.persistence.entity.VehicleStatus;
import com.coursework.driverservice.infrastructure.web.dto.VehicleDto;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicles", description = "Транспортные средства")
public class VehicleController {

    private final VehicleQueryService vehicleQueryService;
    private final CreateVehicleCommandHandler createVehicleCommandHandler;
    private final UpdateVehicleCommandHandler updateVehicleCommandHandler;
    private final DeleteVehicleCommandHandler deleteVehicleCommandHandler;

    @GetMapping
    @Operation(summary = "Список ТС")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Страница ТС",
                    content = @Content(schema = @Schema(implementation = VehicleDto.class)))
    })
    public Page<VehicleDto> list(
            @Parameter(description = "Статус ТС") @RequestParam(required = false) VehicleStatus status,
            Pageable pageable
    ) {
        return vehicleQueryService.search(status, pageable);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('DISPATCHER')")
    @Operation(summary = "Создать ТС")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Создано"),
            @ApiResponse(responseCode = "400", description = "Валидация"),
            @ApiResponse(responseCode = "409", description = "Конфликт")
    })
    public ResponseEntity<VehicleDto> create(@Valid @RequestBody CreateVehicleCommand body) {
        VehicleDto created = createVehicleCommandHandler.handle(body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/vehicles/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Обновить ТС")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Обновлено"),
            @ApiResponse(responseCode = "404", description = "Не найдено")
    })
    public VehicleDto update(
            @Parameter(description = "ID ТС") @PathVariable Long id,
            @Valid @RequestBody UpdateVehicleCommand body
    ) {
        return updateVehicleCommandHandler.handle(id, body);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить ТС")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Удалено"),
            @ApiResponse(responseCode = "409", description = "Есть незавершённые рейсы")
    })
    public ResponseEntity<Void> delete(@Parameter(description = "ID ТС") @PathVariable Long id) {
        deleteVehicleCommandHandler.handle(id);
        return ResponseEntity.noContent().build();
    }
}
