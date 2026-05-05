package com.coursework.driverservice.infrastructure.web.mapper;

import com.coursework.driverservice.infrastructure.persistence.entity.TripEntity;
import com.coursework.driverservice.infrastructure.web.dto.TripDetailsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TripDetailsAssembler {

    private final TripMapper tripMapper;
    private final RouteMapper routeMapper;

    public TripDetailsDto toDetailsDto(TripEntity entity) {
        return new TripDetailsDto(
                tripMapper.toDto(entity),
                entity.getRoute() == null ? null : routeMapper.toDto(entity.getRoute()),
                entity.getRoutePoints().stream().map(routeMapper::toDto).toList()
        );
    }
}
