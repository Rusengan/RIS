package com.coursework.driverservice.application.handler;

import com.coursework.driverservice.application.command.AcceptTripCommand;
import com.coursework.driverservice.infrastructure.persistence.entity.TripEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.TripStatus;
import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionStatus;
import com.coursework.driverservice.infrastructure.persistence.repository.TripRepository;
import com.coursework.driverservice.infrastructure.persistence.repository.WorkSessionRepository;
import com.coursework.driverservice.infrastructure.web.dto.TripDto;
import com.coursework.driverservice.infrastructure.web.exception.BusinessRuleException;
import com.coursework.driverservice.infrastructure.web.mapper.TripMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AcceptTripCommandHandler {

    private final TripRepository tripRepository;
    private final WorkSessionRepository workSessionRepository;
    private final TripMapper tripMapper;

    @Transactional
    public TripDto handle(AcceptTripCommand cmd) {
        TripEntity trip = tripRepository.findById(cmd.tripId())
                .orElseThrow(() -> new BusinessRuleException("TRIP_NOT_FOUND"));

        if (!trip.getDriver().getId().equals(cmd.driverId())) {
            throw new BusinessRuleException("TRIP_OWNERSHIP_MISMATCH");
        }

        if (trip.getStatus() != TripStatus.PLANNED) {
            throw new BusinessRuleException("TRIP_NOT_PLANNED");
        }

        WorkSessionEntity session = workSessionRepository
                .findByDriverIdAndStatus(cmd.driverId(), WorkSessionStatus.OPEN)
                .orElseThrow(() -> new BusinessRuleException("NO_OPEN_SESSION"));

        trip.setWorkSession(session);
        trip.setStatus(TripStatus.IN_PROGRESS);
        trip.setActualStartAt(Instant.now());

        TripEntity saved = tripRepository.save(trip);
        return tripMapper.toDto(saved);
    }
}
