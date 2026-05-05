package com.coursework.driverservice.application.handler;

import com.coursework.driverservice.application.command.StartWorkSessionCommand;
import com.coursework.driverservice.infrastructure.persistence.entity.UserEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionStatus;
import com.coursework.driverservice.infrastructure.persistence.repository.UserRepository;
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
class StartWorkSessionCommandHandlerTest {

    @Mock
    private WorkSessionRepository workSessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkSessionMapper workSessionMapper;

    @InjectMocks
    private StartWorkSessionCommandHandler handler;

    @Test
    void handle_shouldThrowException_whenOpenSessionAlreadyExists() {
        // Arrange
        Long driverId = 1L;
        StartWorkSessionCommand command = new StartWorkSessionCommand(driverId);
        
        WorkSessionEntity existingSession = WorkSessionEntity.builder()
                .id(1L)
                .status(WorkSessionStatus.OPEN)
                .build();
        
        when(workSessionRepository.findByDriverIdAndStatus(driverId, WorkSessionStatus.OPEN))
                .thenReturn(Optional.of(existingSession));

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("WORK_SESSION_ALREADY_OPEN");

        verify(workSessionRepository).findByDriverIdAndStatus(driverId, WorkSessionStatus.OPEN);
        verifyNoMoreInteractions(workSessionRepository, userRepository, workSessionMapper);
    }

    @Test
    void handle_shouldThrowException_whenDriverNotFound() {
        // Arrange
        Long driverId = 1L;
        StartWorkSessionCommand command = new StartWorkSessionCommand(driverId);
        
        when(workSessionRepository.findByDriverIdAndStatus(driverId, WorkSessionStatus.OPEN))
                .thenReturn(Optional.empty());
        when(userRepository.findById(driverId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("DRIVER_NOT_FOUND");

        verify(workSessionRepository).findByDriverIdAndStatus(driverId, WorkSessionStatus.OPEN);
        verify(userRepository).findById(driverId);
        verifyNoMoreInteractions(workSessionRepository, userRepository, workSessionMapper);
    }

    @Test
    void handle_shouldCreateNewSession_whenNoOpenSessionExists() {
        // Arrange
        Long driverId = 1L;
        StartWorkSessionCommand command = new StartWorkSessionCommand(driverId);
        
        UserEntity driver = UserEntity.builder()
                .id(driverId)
                .email("driver@example.com")
                .fullName("Driver Name")
                .build();
        
        WorkSessionEntity savedSession = WorkSessionEntity.builder()
                .id(1L)
                .driver(driver)
                .startedAt(Instant.now())
                .status(WorkSessionStatus.OPEN)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        WorkSessionDto expectedDto = new WorkSessionDto(
                1L, driverId, savedSession.getStartedAt(), null, null,
                WorkSessionStatus.OPEN, savedSession.getCreatedAt(), savedSession.getUpdatedAt(),
                List.of()
        );

        when(workSessionRepository.findByDriverIdAndStatus(driverId, WorkSessionStatus.OPEN))
                .thenReturn(Optional.empty());
        when(userRepository.findById(driverId))
                .thenReturn(Optional.of(driver));
        when(userRepository.getReferenceById(driverId))
                .thenReturn(driver);
        when(workSessionRepository.save(any(WorkSessionEntity.class)))
                .thenReturn(savedSession);
        when(workSessionMapper.toDto(savedSession))
                .thenReturn(expectedDto);

        // Act
        WorkSessionDto result = handler.handle(command);

        // Assert
        assertThat(result).isEqualTo(expectedDto);
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.driverId()).isEqualTo(driverId);
        assertThat(result.status()).isEqualTo(WorkSessionStatus.OPEN);
        assertThat(result.startedAt()).isNotNull();
        assertThat(result.endedAt()).isNull();
        assertThat(result.totalMinutes()).isNull();

        verify(workSessionRepository).findByDriverIdAndStatus(driverId, WorkSessionStatus.OPEN);
        verify(userRepository).findById(driverId);
        verify(userRepository).getReferenceById(driverId);
        verify(workSessionRepository).save(any(WorkSessionEntity.class));
        verify(workSessionMapper).toDto(savedSession);
    }
}