package com.coursework.driverservice.application.handler;

import com.coursework.driverservice.application.command.StartWorkSessionCommand;
import com.coursework.driverservice.domain.audit.WorkSessionStartedEvent;
import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionStatus;
import com.coursework.driverservice.infrastructure.persistence.repository.UserRepository;
import com.coursework.driverservice.infrastructure.persistence.repository.WorkSessionRepository;
import com.coursework.driverservice.infrastructure.web.dto.WorkSessionDto;
import com.coursework.driverservice.infrastructure.web.exception.BusinessRuleException;
import com.coursework.driverservice.infrastructure.web.mapper.WorkSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StartWorkSessionCommandHandler {

    private final WorkSessionRepository workSessionRepository;
    private final UserRepository userRepository;
    private final WorkSessionMapper workSessionMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public WorkSessionDto handle(StartWorkSessionCommand cmd) {
        // Проверяем, есть ли уже открытая сессия у водителя
        workSessionRepository.findByDriverIdAndStatus(cmd.driverId(), WorkSessionStatus.OPEN)
                .ifPresent(session -> {
                    throw new BusinessRuleException("WORK_SESSION_ALREADY_OPEN");
                });

        // Проверяем существование водителя
        userRepository.findById(cmd.driverId())
                .orElseThrow(() -> new BusinessRuleException("DRIVER_NOT_FOUND"));

        // Создаём новую сессию
        WorkSessionEntity session = WorkSessionEntity.builder()
                .driver(userRepository.getReferenceById(cmd.driverId()))
                .startedAt(Instant.now())
                .status(WorkSessionStatus.OPEN)
                .build();

        WorkSessionEntity saved = workSessionRepository.save(session);
        eventPublisher.publishEvent(new WorkSessionStartedEvent(
                cmd.driverId(),
                saved.getId(),
                Map.of("startedAt", saved.getStartedAt().toString())
        ));
        return workSessionMapper.toDto(saved);
    }
}