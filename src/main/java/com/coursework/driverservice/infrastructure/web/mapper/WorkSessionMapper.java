package com.coursework.driverservice.infrastructure.web.mapper;

import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionEntity;
import com.coursework.driverservice.infrastructure.web.dto.WorkSessionDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = BreakLogMapper.class)
public interface WorkSessionMapper {

    @Mapping(target = "driverId", source = "driver.id")
    WorkSessionDto toDto(WorkSessionEntity entity);
}