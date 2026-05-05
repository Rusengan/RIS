package com.coursework.driverservice.application.handler;

import com.coursework.driverservice.application.command.CloseWorkSessionCommand;
import com.coursework.driverservice.infrastructure.persistence.entity.TripStatus;
import com.coursework.driverservice.infrastructure.persistence.entity.UserEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionStatus;
import com.coursework.driverservice.infrastructure.persistence.repository.TripRepository;
import com.coursework.driverservice.infrastructure.persistence.repository.WorkSessionRepository;
import com.coursework.driverservice.infrastructure.web.dto.WorkSessionDto;
import com.coursework.driverservice.infrastructure.web.exception.BusinessRuleException;
import com.coursework.driverservice.infrastructure.web.mapper.WorkSessionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloseWorkSessionCommandHandlerTest {

    @Mock
    private WorkSessionRepository workSessionRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private WorkSessionMapper workSessionMapper;

    @InjectMocks
    private CloseWorkSessionCommandHandler handler;

    @Test
    void handle_shouldThrowException_whenSessionNotFound() {
        // Arrange
        Long sessionId = 1L;
        Long driverId = 1L;
        CloseWorkSessionCommand command = new CloseWorkSessionCommand(sessionId, driverId);
        
        when(workSessionRepository.findById(sessionId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("WORK_SESSION_NOT_FOUND");

        verify(workSessionRepository).findById(sessionId);
        verifyNoMoreInteractions(workSessionRepository, tripRepository, workSessionMapper);
    }

    @Test
    void handle_shouldThrowException_whenOwnershipMismatch() {
        // Arrange
        Long sessionId = 1L;
        Long driverId = 1L;
        Long otherDriverId = 2L;
        CloseWorkSessionCommand command = new CloseWorkSessionCommand(sessionId, driverId);
        
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
        verifyNoMoreInteractions(workSessionRepository, tripRepository, workSessionMapper);
    }

    @Test
    void handle_shouldThrowException_whenSessionNotOpen() {
        // Arrange
        Long sessionId = 1L;
        Long driverId = 1L;
        CloseWorkSessionCommand command = new CloseWorkSessionCommand(sessionId, driverId);
        
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
        verifyNoMoreInteractions(workSessionRepository, tripRepository, workSessionMapper);
    }

    @Test
    void handle_shouldThrowException_whenActiveTripExists() {
        // Arrange
        Long sessionId = 1L;
        Long driverId = 1L;
        CloseWorkSessionCommand command = new CloseWorkSessionCommand(sessionId, driverId);
        
        UserEntity driver = UserEntity.builder()
                .id(driverId)
                .build();
        
        WorkSessionEntity session = WorkSessionEntity.builder()
                .id(sessionId)
                .driver(driver)
                .status(WorkSessionStatus.OPEN)
                .startedAt(Instant.now().minus(2, java.time.temporal.ChronoUnit.HOURS))
                .build();
        
        when(workSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));
        when(tripRepository.existsByDriverIdAndStatus(driverId, TripStatus.IN_PROGRESS))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("ACTIVE_TRIP_EXISTS");

        verify(workSessionRepository).findById(sessionId);
        verify(tripRepository).existsByDriverIdAndStatus(driverId, TripStatus.IN_PROGRESS);
        verifyNoMoreInteractions(workSessionRepository, tripRepository, workSessionMapper);
    }

    @Test
    void handle_shouldCloseSessionSuccessfully() {
        // Arrange
        Long sessionId = 1L;
        Long driverId = 1L;
        CloseWorkSessionCommand command = new CloseWorkSessionCommand(sessionId, driverId);
        
        Instant startedAt = Instant.now().minus(2, java.time.temporal.ChronoUnit.HOURS);
        
        UserEntity driver = UserEntity.builder()
                .id(driverId)
                .build();
        
        WorkSessionEntity session = WorkSessionEntity.builder()
                .id(sessionId)
                .driver(driver)
                .status(WorkSessionStatus.OPEN)
                .startedAt(startedAt)
                .build();
        
        WorkSessionEntity savedSession = WorkSessionEntity.builder()
                .id(sessionId)
                .driver(driver)
                .status(WorkSessionStatus.CLOSED)
                .startedAt(startedAt)
                .endedAt(Instant.now())
                .totalMinutes(120)
                .build();
        
        WorkSessionDto expectedDto = new WorkSessionDto(
                sessionId, driverId, startedAt, savedSession.getEndedAt(),
                120, WorkSessionStatus.CLOSED, null, null,
                List.of()
        );

        when(workSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));
        when(tripRepository.existsByDriverIdAndStatus(driverId, TripStatus.IN_PROGRESS))
                .thenReturn(false);
        when(workSessionRepository.save(any(WorkSessionEntity.class)))
                .thenReturn(savedSession);
        when(workSessionMapper.toDto(savedSession))
                .thenReturn(expectedDto);

        // Act
        WorkSessionDto result = handler.handle(command);

        // Assert
        assertThat(result).isEqualTo(expectedDto);
        assertThat(result.id()).isEqualTo(sessionId);
        assertThat(result.driverId()).isEqualTo(driverId);
        assertThat(result.status()).isEqualTo(WorkSessionStatus.CLOSED);
        assertThat(result.endedAt()).isNotNull();
        assertThat(result.totalMinutes()).isEqualTo(120);

        verify(workSessionRepository).findById(sessionId);
        verify(tripRepository).existsByDriverIdAndStatus(driverId, TripStatus.IN_PROGRESS);
        verify(workSessionRepository).save(any(WorkSessionEntity.class));
        verify(workSessionMapper).toDto(savedSession);
    }
}