package com.coursework.driverservice.application.handler;

import com.coursework.driverservice.application.command.StartBreakCommand;
import com.coursework.driverservice.infrastructure.persistence.entity.BreakLogEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.BreakType;
import com.coursework.driverservice.infrastructure.persistence.entity.UserEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionStatus;
import com.coursework.driverservice.infrastructure.persistence.repository.BreakLogRepository;
import com.coursework.driverservice.infrastructure.persistence.repository.WorkSessionRepository;
import com.coursework.driverservice.infrastructure.web.dto.BreakLogDto;
import com.coursework.driverservice.infrastructure.web.exception.BusinessRuleException;
import com.coursework.driverservice.infrastructure.web.mapper.BreakLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartBreakCommandHandlerTest {

    @Mock
    private WorkSessionRepository workSessionRepository;

    @Mock
    private BreakLogRepository breakLogRepository;

    @Mock
    private BreakLogMapper breakLogMapper;

    @InjectMocks
    private StartBreakCommandHandler handler;

    @Test
    void handle_shouldThrowException_whenSessionNotFound() {
        // Arrange
        Long sessionId = 1L;
        Long driverId = 1L;
        StartBreakCommand command = new StartBreakCommand(sessionId, driverId, BreakType.SHORT);
        
        when(workSessionRepository.findById(sessionId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("WORK_SESSION_NOT_FOUND");

        verify(workSessionRepository).findById(sessionId);
        verifyNoMoreInteractions(workSessionRepository, breakLogRepository, breakLogMapper);
    }

    @Test
    void handle_shouldThrowException_whenOwnershipMismatch() {
        // Arrange
        Long sessionId = 1L;
        Long driverId = 1L;
        Long otherDriverId = 2L;
        StartBreakCommand command = new StartBreakCommand(sessionId, driverId, BreakType.SHORT);
        
        UserEntity otherDriver = UserEntity.builder()
                .id(otherDriverId)
                .build();
        
        WorkSessionEntity session = WorkSessionEntity.builder()
                .id(sessionId)
                .driver(otherDriver)
                .status(WorkSessionStatus.OPEN)
                .build();
        
        when(workSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("WORK_SESSION_OWNERSHIP_MISMATCH");

        verify(workSessionRepository).findById(sessionId);
        verifyNoMoreInteractions(workSessionRepository, breakLogRepository, breakLogMapper);
    }

    @Test
    void handle_shouldThrowException_whenSessionNotOpen() {
        // Arrange
        Long sessionId = 1L;
        Long driverId = 1L;
        StartBreakCommand command = new StartBreakCommand(sessionId, driverId, BreakType.SHORT);
        
        UserEntity driver = UserEntity.builder()
                .id(driverId)
                .build();
        
        WorkSessionEntity session = WorkSessionEntity.builder()
                .id(sessionId)
                .driver(driver)
                .status(WorkSessionStatus.CLOSED)
                .build();
        
        when(workSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("WORK_SESSION_NOT_OPEN");

        verify(workSessionRepository).findById(sessionId);
        verifyNoMoreInteractions(workSessionRepository, breakLogRepository, breakLogMapper);
    }

    @Test
    void handle_shouldThrowException_whenActiveBreakExists() {
        // Arrange
        Long sessionId = 1L;
        Long driverId = 1L;
        StartBreakCommand command = new StartBreakCommand(sessionId, driverId, BreakType.SHORT);
        
        UserEntity driver = UserEntity.builder()
                .id(driverId)
                .build();
        
        WorkSessionEntity session = WorkSessionEntity.builder()
                .id(sessionId)
                .driver(driver)
                .status(WorkSessionStatus.OPEN)
                .build();
        
        BreakLogEntity activeBreak = BreakLogEntity.builder()
                .id(1L)
                .build();
        
        when(workSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));
        when(breakLogRepository.findFirstByWorkSessionIdAndEndedAtIsNull(sessionId))
                .thenReturn(Optional.of(activeBreak));

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("ACTIVE_BREAK_EXISTS");

        verify(workSessionRepository).findById(sessionId);
        verify(breakLogRepository).findFirstByWorkSessionIdAndEndedAtIsNull(sessionId);
        verifyNoMoreInteractions(workSessionRepository, breakLogRepository, breakLogMapper);
    }

    @Test
    void handle_shouldCreateBreakSuccessfully() {
        // Arrange
        Long sessionId = 1L;
        Long driverId = 1L;
        BreakType breakType = BreakType.LUNCH;
        StartBreakCommand command = new StartBreakCommand(sessionId, driverId, breakType);
        
        UserEntity driver = UserEntity.builder()
                .id(driverId)
                .build();
        
        WorkSessionEntity session = WorkSessionEntity.builder()
                .id(sessionId)
                .driver(driver)
                .status(WorkSessionStatus.OPEN)
                .build();
        
        BreakLogEntity savedBreak = BreakLogEntity.builder()
                .id(1L)
                .workSession(session)
                .breakType(breakType)
                .startedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        BreakLogDto expectedDto = new BreakLogDto(
                1L, sessionId, breakType, savedBreak.getStartedAt(), 
                null, null, savedBreak.getCreatedAt(), savedBreak.getUpdatedAt()
        );

        when(workSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));
        when(breakLogRepository.findFirstByWorkSessionIdAndEndedAtIsNull(sessionId))
                .thenReturn(Optional.empty());
        when(breakLogRepository.save(any(BreakLogEntity.class)))
                .thenReturn(savedBreak);
        when(breakLogMapper.toDto(savedBreak))
                .thenReturn(expectedDto);

        // Act
        BreakLogDto result = handler.handle(command);

        // Assert
        assertThat(result).isEqualTo(expectedDto);
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.workSessionId()).isEqualTo(sessionId);
        assertThat(result.breakType()).isEqualTo(breakType);
        assertThat(result.startedAt()).isNotNull();
        assertThat(result.endedAt()).isNull();
        assertThat(result.durationMinutes()).isNull();

        verify(workSessionRepository).findById(sessionId);
        verify(breakLogRepository).findFirstByWorkSessionIdAndEndedAtIsNull(sessionId);
        verify(breakLogRepository).save(any(BreakLogEntity.class));
        verify(breakLogMapper).toDto(savedBreak);
    }
}