package com.coursework.driverservice.application.handler;

import com.coursework.driverservice.application.command.EndBreakCommand;
import com.coursework.driverservice.infrastructure.persistence.entity.BreakLogEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionStatus;
import com.coursework.driverservice.infrastructure.persistence.repository.BreakLogRepository;
import com.coursework.driverservice.infrastructure.web.dto.BreakLogDto;
import com.coursework.driverservice.infrastructure.web.exception.BusinessRuleException;
import com.coursework.driverservice.infrastructure.web.mapper.BreakLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class EndBreakCommandHandler {

    private final BreakLogRepository breakLogRepository;
    private final BreakLogMapper breakLogMapper;

    @Transactional
    public BreakLogDto handle(EndBreakCommand cmd) {
        // Загружаем перерыв
        BreakLogEntity breakLog = breakLogRepository.findById(cmd.breakId())
                .orElseThrow(() -> new BusinessRuleException("BREAK_LOG_NOT_FOUND"));

        // Проверяем ownership через сессию
        WorkSessionEntity session = breakLog.getWorkSession();
        if (!session.getDriver().getId().equals(cmd.driverId())) {
            throw new BusinessRuleException("BREAK_LOG_OWNERSHIP_MISMATCH");
        }

        // Проверяем, что сессия открыта
        if (session.getStatus() != WorkSessionStatus.OPEN) {
            throw new BusinessRuleException("WORK_SESSION_NOT_OPEN");
        }

        // Проверяем, что перерыв ещё не закрыт
        if (breakLog.getEndedAt() != null) {
            throw new BusinessRuleException("BREAK_ALREADY_ENDED");
        }

        // Закрываем перерыв
        Instant endedAt = Instant.now();
        breakLog.setEndedAt(endedAt);

        // Рассчитываем продолжительность
        long durationMinutes = ChronoUnit.MINUTES.between(breakLog.getStartedAt(), endedAt);
        breakLog.setDurationMinutes((int) durationMinutes);

        // Сохраняем изменения
        BreakLogEntity saved = breakLogRepository.save(breakLog);
        return breakLogMapper.toDto(saved);
    }
}