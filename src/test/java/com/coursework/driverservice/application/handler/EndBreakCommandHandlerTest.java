package com.coursework.driverservice.application.handler;

import com.coursework.driverservice.application.command.EndBreakCommand;
import com.coursework.driverservice.infrastructure.persistence.entity.BreakLogEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.BreakType;
import com.coursework.driverservice.infrastructure.persistence.entity.UserEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionStatus;
import com.coursework.driverservice.infrastructure.persistence.repository.BreakLogRepository;
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
class EndBreakCommandHandlerTest {

    @Mock
    private BreakLogRepository breakLogRepository;

    @Mock
    private BreakLogMapper breakLogMapper;

    @InjectMocks
    private EndBreakCommandHandler handler;

    @Test
    void handle_shouldThrowException_whenBreakNotFound() {
        // Arrange
        Long breakId = 1L;
        Long driverId = 1L;
        EndBreakCommand command = new EndBreakCommand(breakId, driverId);
        
        when(breakLogRepository.findById(breakId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("BREAK_LOG_NOT_FOUND");

        verify(breakLogRepository).findById(breakId);
        verifyNoMoreInteractions(breakLogRepository, breakLogMapper);
    }

    @Test
    void handle_shouldThrowException_whenOwnershipMismatch() {
        // Arrange
        Long breakId = 1L;
        Long driverId = 1L;
        Long otherDriverId = 2L;
        EndBreakCommand command = new EndBreakCommand(breakId, driverId);
        
        UserEntity otherDriver = UserEntity.builder()
                .id(otherDriverId)
                .build();
        
        WorkSessionEntity session = WorkSessionEntity.builder()
                .driver(otherDriver)
                .status(WorkSessionStatus.OPEN)
                .build();
        
        BreakLogEntity breakLog = BreakLogEntity.builder()
                .id(breakId)
                .workSession(session)
                .build();
        
        when(breakLogRepository.findById(breakId))
                .thenReturn(Optional.of(breakLog));

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("BREAK_LOG_OWNERSHIP_MISMATCH");

        verify(breakLogRepository).findById(breakId);
        verifyNoMoreInteractions(breakLogRepository, breakLogMapper);
    }

    @Test
    void handle_shouldThrowException_whenSessionNotOpen() {
        // Arrange
        Long breakId = 1L;
        Long driverId = 1L;
        EndBreakCommand command = new EndBreakCommand(breakId, driverId);
        
        UserEntity driver = UserEntity.builder()
                .id(driverId)
                .build();
        
        WorkSessionEntity session = WorkSessionEntity.builder()
                .driver(driver)
                .status(WorkSessionStatus.CLOSED)
                .build();
        
        BreakLogEntity breakLog = BreakLogEntity.builder()
                .id(breakId)
                .workSession(session)
                .build();
        
        when(breakLogRepository.findById(breakId))
                .thenReturn(Optional.of(breakLog));

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("WORK_SESSION_NOT_OPEN");

        verify(breakLogRepository).findById(breakId);
        verifyNoMoreInteractions(breakLogRepository, breakLogMapper);
    }

    @Test
    void handle_shouldThrowException_whenBreakAlreadyEnded() {
        // Arrange
        Long breakId = 1L;
        Long driverId = 1L;
        EndBreakCommand command = new EndBreakCommand(breakId, driverId);
        
        UserEntity driver = UserEntity.builder()
                .id(driverId)
                .build();
        
        WorkSessionEntity session = WorkSessionEntity.builder()
                .driver(driver)
                .status(WorkSessionStatus.OPEN)
                .build();
        
        BreakLogEntity breakLog = BreakLogEntity.builder()
                .id(breakId)
                .workSession(session)
                .endedAt(Instant.now())
                .durationMinutes(30)
                .build();
        
        when(breakLogRepository.findById(breakId))
                .thenReturn(Optional.of(breakLog));

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("BREAK_ALREADY_ENDED");

        verify(breakLogRepository).findById(breakId);
        verifyNoMoreInteractions(breakLogRepository, breakLogMapper);
    }

    @Test
    void handle_shouldEndBreakSuccessfully() {
        // Arrange
        Long breakId = 1L;
        Long driverId = 1L;
        EndBreakCommand command = new EndBreakCommand(breakId, driverId);
        
        Instant startedAt = Instant.now().minus(30, java.time.temporal.ChronoUnit.MINUTES);
        
        UserEntity driver = UserEntity.builder()
                .id(driverId)
                .build();
        
        WorkSessionEntity session = WorkSessionEntity.builder()
                .driver(driver)
                .status(WorkSessionStatus.OPEN)
                .build();
        
        BreakLogEntity breakLog = BreakLogEntity.builder()
                .id(breakId)
                .workSession(session)
                .breakType(BreakType.LUNCH)
                .startedAt(startedAt)
                .build();
        
        BreakLogEntity savedBreak = BreakLogEntity.builder()
                .id(breakId)
                .workSession(session)
                .breakType(BreakType.LUNCH)
                .startedAt(startedAt)
                .endedAt(Instant.now())
                .durationMinutes(30)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        BreakLogDto expectedDto = new BreakLogDto(
                breakId, session.getId(), BreakType.LUNCH, startedAt, 
                savedBreak.getEndedAt(), 30, savedBreak.getCreatedAt(), savedBreak.getUpdatedAt()
        );

        when(breakLogRepository.findById(breakId))
                .thenReturn(Optional.of(breakLog));
        when(breakLogRepository.save(any(BreakLogEntity.class)))
                .thenReturn(savedBreak);
        when(breakLogMapper.toDto(savedBreak))
                .thenReturn(expectedDto);

        // Act
        BreakLogDto result = handler.handle(command);

        // Assert
        assertThat(result).isEqualTo(expectedDto);
        assertThat(result.id()).isEqualTo(breakId);
        assertThat(result.breakType()).isEqualTo(BreakType.LUNCH);
        assertThat(result.startedAt()).isEqualTo(startedAt);
        assertThat(result.endedAt()).isNotNull();
        assertThat(result.durationMinutes()).isEqualTo(30);

        verify(breakLogRepository).findById(breakId);
        verify(breakLogRepository).save(any(BreakLogEntity.class));
        verify(breakLogMapper).toDto(savedBreak);
    }
}