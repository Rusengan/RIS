package com.coursework.driverservice.application.handler;

import com.coursework.driverservice.application.command.CheckInRoutePointCommand;
import com.coursework.driverservice.infrastructure.persistence.entity.RoutePointEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.TripEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.TripStatus;
import com.coursework.driverservice.infrastructure.persistence.repository.RoutePointRepository;
import com.coursework.driverservice.infrastructure.web.dto.RoutePointDto;
import com.coursework.driverservice.infrastructure.web.exception.BusinessRuleException;
import com.coursework.driverservice.infrastructure.web.mapper.RouteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CheckInRoutePointCommandHandler {

    private final RoutePointRepository routePointRepository;
    private final RouteMapper routeMapper;

    @Transactional
    public RoutePointDto handle(CheckInRoutePointCommand cmd) {
        RoutePointEntity point = routePointRepository.findById(cmd.routePointId())
                .orElseThrow(() -> new BusinessRuleException("ROUTE_POINT_NOT_FOUND"));

        TripEntity trip = point.getTrip();
        if (!trip.getDriver().getId().equals(cmd.driverId())) {
            throw new BusinessRuleException("TRIP_OWNERSHIP_MISMATCH");
        }

        if (trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new BusinessRuleException("TRIP_NOT_IN_PROGRESS");
        }

        if (point.getArrivedAt() != null) {
            throw new BusinessRuleException("ROUTE_POINT_ALREADY_VISITED");
        }

        point.setArrivedAt(Instant.now());
        RoutePointEntity saved = routePointRepository.save(point);
        return routeMapper.toDto(saved);
    }
}
