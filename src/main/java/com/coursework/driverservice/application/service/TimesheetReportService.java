package com.coursework.driverservice.application.service;

import com.coursework.driverservice.infrastructure.persistence.entity.UserEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionEntity;
import com.coursework.driverservice.infrastructure.persistence.repository.BreakLogRepository;
import com.coursework.driverservice.infrastructure.persistence.repository.UserRepository;
import com.coursework.driverservice.infrastructure.persistence.repository.WorkSessionRepository;
import com.coursework.driverservice.infrastructure.web.dto.TimesheetReportDto;
import com.coursework.driverservice.infrastructure.web.dto.WorkSessionDto;
import com.coursework.driverservice.infrastructure.web.exception.NotFoundException;
import com.coursework.driverservice.infrastructure.web.mapper.WorkSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimesheetReportService {

    private final WorkSessionRepository workSessionRepository;
    private final BreakLogRepository breakLogRepository;
    private final UserRepository userRepository;
    private final WorkSessionMapper workSessionMapper;

    @Transactional(readOnly = true)
    public TimesheetReportDto generate(Long driverId, Instant from, Instant to) {
        // Получаем информацию о водителе
        UserEntity driver = userRepository.findById(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found: " + driverId));

        // Получаем список сессий за период
        List<WorkSessionEntity> sessions = workSessionRepository.findByDriverIdAndStartedAtBetween(driverId, from, to);
        
        // Конвертируем в DTO
        List<WorkSessionDto> sessionDtos = sessions.stream()
                .map(workSessionMapper::toDto)
                .toList();

        // Получаем агрегированные данные
        Integer totalWorkMinutes = workSessionRepository.sumTotalMinutesByDriverIdAndDateRange(driverId, from, to);
        Long sessionsCount = workSessionRepository.countByDriverIdAndDateRange(driverId, from, to);
        Integer totalBreakMinutes = breakLogRepository.sumBreakDurationByDriverIdAndDateRange(driverId, from, to);

        return new TimesheetReportDto(
                driverId,
                driver.getFullName(),
                from,
                to,
                totalWorkMinutes != null ? totalWorkMinutes : 0,
                totalBreakMinutes != null ? totalBreakMinutes : 0,
                sessionsCount != null ? sessionsCount.intValue() : 0,
                sessionDtos
        );
    }
}