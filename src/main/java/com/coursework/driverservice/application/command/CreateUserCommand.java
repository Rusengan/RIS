package com.coursework.driverservice.application.command;

import com.coursework.driverservice.infrastructure.persistence.entity.RoleCode;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record CreateUserCommand(
        @NotBlank String email,
        @NotBlank String fullName,
        Set<RoleCode> roles
) {
}
