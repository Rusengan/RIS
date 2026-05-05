package com.coursework.driverservice.application.handler;

import com.coursework.driverservice.application.command.CloseWorkSessionCommand;
import com.coursework.driverservice.domain.audit.WorkSessionClosedEvent;
import com.coursework.driverservice.infrastructure.persistence.entity.TripStatus;
import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionStatus;
import com.coursework.driverservice.infrastructure.persistence.repository.TripRepository;
import com.coursework.driverservice.infrastructure.persistence.repository.WorkSessionRepository;
import com.coursework.driverservice.infrastructure.web.dto.WorkSessionDto;
import com.coursework.driverservice.infrastructure.web.exception.BusinessRuleException;
import com.coursework.driverservice.infrastructure.web.mapper.WorkSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloseWorkSessionCommandHandler {

    private final WorkSessionRepository workSessionRepository;
    private final TripRepository tripRepository;
    private final WorkSessionMapper workSessionMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public WorkSessionDto handle(CloseWorkSessionCommand cmd) {
        // Загружаем сессию
        WorkSessionEntity session = workSessionRepository.findById(cmd.sessionId())
                .orElseThrow(() -> new BusinessRuleException("WORK_SESSION_NOT_FOUND"));

        // Проверяем ownership
        if (!session.getDriver().getId().equals(cmd.driverId())) {
            throw new BusinessRuleException("WORK_SESSION_OWNERSHIP_MISMATCH");
        }

        // Проверяем статус
        if (session.getStatus() != WorkSessionStatus.OPEN) {
            throw new BusinessRuleException("WORK_SESSION_NOT_OPEN");
        }

        // Проверяем, что нет активных рейсов у этого водителя
        if (tripRepository.existsByDriverIdAndStatus(cmd.driverId(), TripStatus.IN_PROGRESS)) {
            throw new BusinessRuleException("ACTIVE_TRIP_EXISTS");
        }

        // Устанавливаем время окончания
        Instant endedAt = Instant.now();
        session.setEndedAt(endedAt);

        // Рассчитываем общее время работы
        long totalWorkMinutes = ChronoUnit.MINUTES.between(session.getStartedAt(), endedAt);

        // Вычитаем время перерывов
        long totalBreakMinutes = session.getBreaks().stream()
                .filter(breakLog -> breakLog.getEndedAt() != null && breakLog.getDurationMinutes() != null)
                .mapToInt(breakLog -> breakLog.getDurationMinutes())
                .sum();

        long netWorkMinutes = totalWorkMinutes - totalBreakMinutes;
        session.setTotalMinutes((int) netWorkMinutes);

        // Устанавливаем статус
        session.setStatus(WorkSessionStatus.CLOSED);

        // Сохраняем изменения
        WorkSessionEntity saved = workSessionRepository.save(session);
        eventPublisher.publishEvent(new WorkSessionClosedEvent(
                cmd.driverId(),
                saved.getId(),
                Map.of(
                        "totalMinutes", saved.getTotalMinutes() != null ? saved.getTotalMinutes() : 0,
                        "endedAt", saved.getEndedAt() != null ? saved.getEndedAt().toString() : ""
                )
        ));
        return workSessionMapper.toDto(saved);
    }
}