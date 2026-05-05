package com.coursework.driverservice.infrastructure.web.mapper;

import com.coursework.driverservice.infrastructure.persistence.entity.TripEntity;
import com.coursework.driverservice.infrastructure.web.dto.TripDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TripMapper {

    @Mapping(target = "driverId", source = "driver.id")
    @Mapping(target = "driverFullName", source = "driver.fullName")
    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "vehiclePlate", source = "vehicle.plateNumber")
    @Mapping(target = "dispatcherId", source = "dispatcher.id")
    @Mapping(target = "workSessionId", source = "workSession.id")
    TripDto toDto(TripEntity entity);
}
