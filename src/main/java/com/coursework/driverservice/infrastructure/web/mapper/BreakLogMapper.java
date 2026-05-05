package com.coursework.driverservice.infrastructure.web.mapper;

import com.coursework.driverservice.infrastructure.persistence.entity.BreakLogEntity;
import com.coursework.driverservice.infrastructure.web.dto.BreakLogDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BreakLogMapper {
    
    @Mapping(target = "workSessionId", source = "workSession.id")
    BreakLogDto toDto(BreakLogEntity entity);
}