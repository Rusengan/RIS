package com.coursework.driverservice.application.handler;

import com.coursework.driverservice.application.command.AppendRoutePointsCommand;
import com.coursework.driverservice.application.command.CreateRoutePointCommand;
import com.coursework.driverservice.infrastructure.persistence.entity.RoutePointEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.TripEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.TripStatus;
import com.coursework.driverservice.infrastructure.persistence.repository.TripRepository;
import com.coursework.driverservice.infrastructure.web.dto.RoutePointDto;
import com.coursework.driverservice.infrastructure.web.exception.BusinessRuleException;
import com.coursework.driverservice.infrastructure.web.mapper.RouteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AppendRoutePointsCommandHandler {

    private final TripRepository tripRepository;
    private final RouteMapper routeMapper;

    @Transactional
    public List<RoutePointDto> handle(AppendRoutePointsCommand cmd) {
        TripEntity trip = tripRepository.findById(cmd.tripId())
                .orElseThrow(() -> new BusinessRuleException("TRIP_NOT_FOUND"));

        if (!trip.getDispatcher().getId().equals(cmd.dispatcherId())) {
            throw new BusinessRuleException("TRIP_DISPATCHER_MISMATCH");
        }

        if (trip.getStatus() != TripStatus.PLANNED) {
            throw new BusinessRuleException("TRIP_NOT_PLANNED");
        }

        Set<Short> addedSeq = new HashSet<>();
        for (CreateRoutePointCommand p : cmd.points()) {
            RoutePointEntity point = RoutePointEntity.builder()
                    .trip(trip)
                    .sequenceNo(p.sequenceNo())
                    .address(p.address())
                    .latitude(p.latitude())
                    .longitude(p.longitude())
                    .pointType(p.type())
                    .build();
            trip.getRoutePoints().add(point);
            addedSeq.add(p.sequenceNo());
        }

        TripEntity saved = tripRepository.save(trip);

        return saved.getRoutePoints().stream()
                .filter(rp -> addedSeq.contains(rp.getSequenceNo()))
                .map(routeMapper::toDto)
                .toList();
    }
}
