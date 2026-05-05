package com.coursework.driverservice.application.command;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserCommand(
        @NotBlank String email,
        @NotBlank String fullName,
        boolean enabled,
        String pictureUrl
) {
}
