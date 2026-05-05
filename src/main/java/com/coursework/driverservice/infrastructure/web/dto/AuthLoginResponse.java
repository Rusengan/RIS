package com.coursework.driverservice.infrastructure.web.dto;

public record AuthLoginResponse(
        String accessToken,
        String refreshToken,
        UserProfileDto user
) {
}
