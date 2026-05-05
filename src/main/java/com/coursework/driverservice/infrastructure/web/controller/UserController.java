package com.coursework.driverservice.infrastructure.web.controller;

import com.coursework.driverservice.application.command.CreateUserCommand;
import com.coursework.driverservice.application.command.UpdateUserCommand;
import com.coursework.driverservice.application.handler.AddUserRoleCommandHandler;
import com.coursework.driverservice.application.handler.CreateUserCommandHandler;
import com.coursework.driverservice.application.handler.UpdateUserCommandHandler;
import com.coursework.driverservice.application.query.UserQueryService;
import com.coursework.driverservice.infrastructure.persistence.entity.RoleCode;
import com.coursework.driverservice.infrastructure.persistence.repository.UserRepository;
import com.coursework.driverservice.infrastructure.web.dto.AddUserRoleRequest;
import com.coursework.driverservice.infrastructure.web.dto.UserDto;
import com.coursework.driverservice.infrastructure.web.exception.NotFoundException;
import com.coursework.driverservice.infrastructure.web.mapper.UserMapper;
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
import org.springframework.security.core.context.SecurityContextHolder;
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
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Пользователи и роли")
public class UserController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserQueryService userQueryService;
    private final CreateUserCommandHandler createUserCommandHandler;
    private final UpdateUserCommandHandler updateUserCommandHandler;
    private final AddUserRoleCommandHandler addUserRoleCommandHandler;

    @GetMapping("/me")
    @Operation(summary = "Текущий пользователь")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "404", description = "Не найден")
    })
    public UserDto me() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByIdWithRoles(userId)
                .map(userMapper::toDto)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Список пользователей")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Страница пользователей"),
            @ApiResponse(responseCode = "403", description = "Нет прав")
    })
    public Page<UserDto> list(
            @Parameter(description = "Фильтр по роли") @RequestParam(required = false) RoleCode role,
            @Parameter(description = "Включён") @RequestParam(required = false) Boolean enabled,
            Pageable pageable
    ) {
        return userQueryService.search(role, enabled, pageable);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создать пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Создан",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "Валидация"),
            @ApiResponse(responseCode = "409", description = "Email занят")
    })
    public ResponseEntity<UserDto> create(@Valid @RequestBody CreateUserCommand body) {
        UserDto created = createUserCommandHandler.handle(body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/users/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Обновить пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Обновлён"),
            @ApiResponse(responseCode = "404", description = "Не найден"),
            @ApiResponse(responseCode = "409", description = "Конфликт email")
    })
    public UserDto update(
            @Parameter(description = "ID пользователя") @PathVariable Long id,
            @Valid @RequestBody UpdateUserCommand body
    ) {
        return updateUserCommandHandler.handle(id, body);
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Добавить роль пользователю")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Роль добавлена"),
            @ApiResponse(responseCode = "404", description = "Не найден")
    })
    public UserDto addRole(
            @Parameter(description = "ID пользователя") @PathVariable Long id,
            @Valid @RequestBody AddUserRoleRequest body
    ) {
        return addUserRoleCommandHandler.handle(id, body.role());
    }
}
