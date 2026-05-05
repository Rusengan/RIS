package com.coursework.driverservice.application.handler;

import com.coursework.driverservice.application.command.CompleteTripCommand;
import com.coursework.driverservice.domain.audit.TripCompletedEvent;
import com.coursework.driverservice.infrastructure.persistence.entity.RouteEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.RoutePointEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.TripEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.TripStatus;
import com.coursework.driverservice.infrastructure.persistence.repository.TripRepository;
import com.coursework.driverservice.infrastructure.web.dto.TripDto;
import com.coursework.driverservice.infrastructure.web.exception.BusinessRuleException;
import com.coursework.driverservice.infrastructure.web.mapper.TripMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CompleteTripCommandHandler {

    private final TripRepository tripRepository;
    private final TripMapper tripMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TripDto handle(CompleteTripCommand cmd) {
        TripEntity trip = tripRepository.findById(cmd.tripId())
                .orElseThrow(() -> new BusinessRuleException("TRIP_NOT_FOUND"));

        if (!trip.getDriver().getId().equals(cmd.driverId())) {
            throw new BusinessRuleException("TRIP_OWNERSHIP_MISMATCH");
        }

        if (trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new BusinessRuleException("TRIP_NOT_IN_PROGRESS");
        }

        for (RoutePointEntity rp : trip.getRoutePoints()) {
            if (rp.getArrivedAt() == null) {
                throw new BusinessRuleException("ROUTE_POINTS_INCOMPLETE");
            }
        }

        RouteEntity route = trip.getRoute();
        if (route == null) {
            throw new BusinessRuleException("ROUTE_NOT_CALCULATED");
        }

        trip.setTotalDistanceM(route.getTotalDistanceM());
        trip.setTotalDurationS(route.getTotalDurationS());
        trip.setStatus(TripStatus.COMPLETED);
        trip.setActualEndAt(Instant.now());

        TripEntity saved = tripRepository.save(trip);
        stringRedisTemplate.delete("trip:active:" + cmd.driverId());
        Map<String, Object> payload = new HashMap<>();
        payload.put("totalDistanceM", saved.getTotalDistanceM());
        payload.put("totalDurationS", saved.getTotalDurationS());
        payload.put("status", saved.getStatus().name());
        eventPublisher.publishEvent(new TripCompletedEvent(cmd.driverId(), saved.getId(), payload));
        return tripMapper.toDto(saved);
    }
}
