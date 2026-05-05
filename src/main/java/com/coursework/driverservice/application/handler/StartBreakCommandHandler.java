package com.coursework.driverservice.application.handler;

import com.coursework.driverservice.application.command.StartBreakCommand;
import com.coursework.driverservice.infrastructure.persistence.entity.BreakLogEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.BreakType;
import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionStatus;
import com.coursework.driverservice.infrastructure.persistence.repository.BreakLogRepository;
import com.coursework.driverservice.infrastructure.persistence.repository.WorkSessionRepository;
import com.coursework.driverservice.infrastructure.web.dto.BreakLogDto;
import com.coursework.driverservice.infrastructure.web.exception.BusinessRuleException;
import com.coursework.driverservice.infrastructure.web.mapper.BreakLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class StartBreakCommandHandler {

    private final WorkSessionRepository workSessionRepository;
    private final BreakLogRepository breakLogRepository;
    private final BreakLogMapper breakLogMapper;

    @Transactional
    public BreakLogDto handle(StartBreakCommand cmd) {
        // Загружаем сессию
        WorkSessionEntity session = workSessionRepository.findById(cmd.sessionId())
                .orElseThrow(() -> new BusinessRuleException("WORK_SESSION_NOT_FOUND"));

        // Проверяем ownership
        if (!session.getDriver().getId().equals(cmd.driverId())) {
            throw new BusinessRuleException("WORK_SESSION_OWNERSHIP_MISMATCH");
        }

        // Проверяем статус сессии
        if (session.getStatus() != WorkSessionStatus.OPEN) {
            throw new BusinessRuleException("WORK_SESSION_NOT_OPEN");
        }

        // Проверяем, что нет активного перерыва
        breakLogRepository.findFirstByWorkSessionIdAndEndedAtIsNull(cmd.sessionId())
                .ifPresent(breakLog -> {
                    throw new BusinessRuleException("ACTIVE_BREAK_EXISTS");
                });

        // Создаём новый перерыв
        BreakLogEntity breakLog = BreakLogEntity.builder()
                .workSession(session)
                .breakType(cmd.breakType())
                .startedAt(Instant.now())
                .build();

        BreakLogEntity saved = breakLogRepository.save(breakLog);
        return breakLogMapper.toDto(saved);
    }
}