package com.coursework.driverservice.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Запись журнала аудита")
public record AuditLogDto(
        @Schema(description = "Идентификатор записи") Long id,
        @Schema(description = "Идентификатор пользователя-инициатора") Long userId,
        @Schema(description = "ФИО пользователя") String userFullName,
        @Schema(description = "Код действия") String action,
        @Schema(description = "Тип сущности") String entityType,
        @Schema(description = "Идентификатор сущности") Long entityId,
        @Schema(description = "JSON полезной нагрузки") String payloadJson,
        @Schema(description = "Время создания") Instant createdAt
) {
}
