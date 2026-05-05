package com.coursework.driverservice.infrastructure.web.dto;

import com.coursework.driverservice.infrastructure.persistence.entity.BreakType;
import jakarta.validation.constraints.NotNull;

public record StartBreakRequest(
        @NotNull BreakType breakType
) {
}