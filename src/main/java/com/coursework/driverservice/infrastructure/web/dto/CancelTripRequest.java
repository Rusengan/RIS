package com.coursework.driverservice.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Отмена рейса")
public record CancelTripRequest(
        @NotBlank String reason
) {
}
