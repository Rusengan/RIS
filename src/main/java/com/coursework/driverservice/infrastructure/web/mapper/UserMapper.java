package com.coursework.driverservice.infrastructure.web.mapper;

import com.coursework.driverservice.application.command.CreateUserCommand;
import com.coursework.driverservice.infrastructure.persistence.entity.RoleEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.UserEntity;
import com.coursework.driverservice.infrastructure.web.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.TreeSet;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "googleSubject", ignore = true)
    @Mapping(target = "pictureUrl", ignore = true)
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "roles", ignore = true)
    UserEntity toEntity(CreateUserCommand command);

    @Mapping(target = "roles", source = "roles", qualifiedByName = "roleCodesFromEntities")
    @Mapping(target = "enabled", expression = "java(Boolean.TRUE.equals(entity.getEnabled()))")
    UserDto toDto(UserEntity entity);

    @Named("roleCodesFromEntities")
    default Set<String> roleCodesFromEntities(Set<RoleEntity> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }
        return roles.stream()
                .map(RoleEntity::getCode)
                .map(Enum::name)
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
