package com.coursework.driverservice.application.handler;

import com.coursework.driverservice.application.command.CalculateRouteCommand;
import com.coursework.driverservice.domain.port.out.GeoPoint;
import com.coursework.driverservice.domain.port.out.RouteCalculationRequest;
import com.coursework.driverservice.domain.port.out.RouteCalculationResult;
import com.coursework.driverservice.domain.port.out.RouteProvider;
import com.coursework.driverservice.infrastructure.persistence.entity.RouteEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.RoutePointEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.RoutePointType;
import com.coursework.driverservice.infrastructure.persistence.entity.RoutingEngine;
import com.coursework.driverservice.infrastructure.persistence.entity.TripEntity;
import com.coursework.driverservice.infrastructure.persistence.repository.RouteRepository;
import com.coursework.driverservice.infrastructure.persistence.repository.TripRepository;
import com.coursework.driverservice.infrastructure.web.dto.RouteDto;
import com.coursework.driverservice.infrastructure.web.exception.BusinessRuleException;
import com.coursework.driverservice.infrastructure.web.mapper.RouteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalculateRouteCommandHandler {

    private final TripRepository tripRepository;
    private final RouteRepository routeRepository;
    private final RouteProvider routeProvider;
    private final RouteMapper routeMapper;

    @Transactional
    public RouteDto handle(CalculateRouteCommand cmd) {
        TripEntity trip = tripRepository.findById(cmd.tripId())
                .orElseThrow(() -> new BusinessRuleException("TRIP_NOT_FOUND"));

        List<RoutePointEntity> ordered = trip.getRoutePoints().stream()
                .sorted(Comparator.comparing(RoutePointEntity::getSequenceNo))
                .toList();

        RoutePointEntity origin = ordered.stream()
                .filter(p -> p.getPointType() == RoutePointType.ORIGIN)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("ROUTE_ORIGIN_MISSING"));

        RoutePointEntity destination = ordered.stream()
                .filter(p -> p.getPointType() == RoutePointType.DESTINATION)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("ROUTE_DESTINATION_MISSING"));

        List<GeoPoint> waypoints = new ArrayList<>();
        for (RoutePointEntity p : ordered) {
            if (p.getPointType() == RoutePointType.WAYPOINT) {
                waypoints.add(new GeoPoint(p.getLatitude(), p.getLongitude()));
            }
        }

        RouteCalculationRequest request = new RouteCalculationRequest(
                new GeoPoint(origin.getLatitude(), origin.getLongitude()),
                new GeoPoint(destination.getLatitude(), destination.getLongitude()),
                waypoints
        );

        RouteCalculationResult result = routeProvider.calculate(request);

        RouteEntity route = routeRepository.findByTripId(trip.getId()).orElseGet(() -> {
            RouteEntity r = RouteEntity.builder().trip(trip).build();
            trip.setRoute(r);
            return r;
        });

        route.setEncodedPolyline(result.encodedPolyline());
        route.setTotalDistanceM(result.distanceMeters());
        route.setTotalDurationS(result.durationSeconds());
        route.setProvider(parseEngine(result.provider()));
        route.setCalculatedAt(result.calculatedAt());

        RouteEntity saved = routeRepository.save(route);
        return routeMapper.toDto(saved);
    }

    private static RoutingEngine parseEngine(String provider) {
        if (provider == null || provider.isBlank()) {
            return RoutingEngine.GOOGLE;
        }
        try {
            return RoutingEngine.valueOf(provider.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return RoutingEngine.GOOGLE;
        }
    }
}
