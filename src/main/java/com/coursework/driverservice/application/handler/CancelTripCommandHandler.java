package com.coursework.driverservice.application.handler;

import com.coursework.driverservice.application.command.CancelTripCommand;
import com.coursework.driverservice.domain.audit.TripCancelledEvent;
import com.coursework.driverservice.infrastructure.persistence.entity.TripEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.TripStatus;
import com.coursework.driverservice.infrastructure.persistence.repository.TripRepository;
import com.coursework.driverservice.infrastructure.web.dto.TripDto;
import com.coursework.driverservice.infrastructure.web.exception.BusinessRuleException;
import com.coursework.driverservice.infrastructure.web.mapper.TripMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CancelTripCommandHandler {

    private final TripRepository tripRepository;
    private final TripMapper tripMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TripDto handle(CancelTripCommand cmd) {
        TripEntity trip = tripRepository.findById(cmd.tripId())
                .orElseThrow(() -> new BusinessRuleException("TRIP_NOT_FOUND"));

        if (trip.getStatus() != TripStatus.PLANNED && trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new BusinessRuleException("TRIP_CANNOT_CANCEL");
        }

        trip.setStatus(TripStatus.CANCELLED);
        trip.setCancelReason(cmd.reason());

        TripEntity saved = tripRepository.save(trip);
        eventPublisher.publishEvent(new TripCancelledEvent(
                cmd.dispatcherId(),
                saved.getId(),
                Map.of("reason", cmd.reason(), "status", saved.getStatus().name())
        ));
        return tripMapper.toDto(saved);
    }
}
