package com.coursework.driverservice.infrastructure.web.mapper;

import com.coursework.driverservice.infrastructure.persistence.entity.RouteEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.RoutePointEntity;
import com.coursework.driverservice.infrastructure.web.dto.RouteDto;
import com.coursework.driverservice.infrastructure.web.dto.RoutePointDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RouteMapper {

    @Mapping(target = "tripId", source = "trip.id")
    @Mapping(target = "provider", expression = "java(entity.getProvider().name())")
    RouteDto toDto(RouteEntity entity);

    @Mapping(target = "tripId", source = "trip.id")
    RoutePointDto toDto(RoutePointEntity entity);
}
