package com.coursework.driverservice.infrastructure.web.dto;

import com.coursework.driverservice.infrastructure.persistence.entity.RoleCode;
import jakarta.validation.constraints.NotNull;

public record AddUserRoleRequest(@NotNull RoleCode role) {
}
