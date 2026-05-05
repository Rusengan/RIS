package com.coursework.driverservice.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Профиль пользователя для UI")
public record UserProfileDto(
        Long id,
        String email,
        String fullName,
        String pictureUrl,
        List<String> roles
) {
}
