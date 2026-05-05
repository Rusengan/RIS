package com.coursework.driverservice.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "Пользователь")
public record UserDto(
        Long id,
        String email,
        String fullName,
        String pictureUrl,
        boolean enabled,
        Set<String> roles
) {
}
