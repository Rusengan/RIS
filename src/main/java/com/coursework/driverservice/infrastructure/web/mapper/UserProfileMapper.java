package com.coursework.driverservice.infrastructure.web.mapper;

import com.coursework.driverservice.infrastructure.persistence.entity.RoleEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.UserEntity;
import com.coursework.driverservice.infrastructure.web.dto.UserProfileDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserProfileMapper {

    public UserProfileDto toDto(UserEntity user) {
        List<String> roles = user.getRoles() == null
                ? List.of()
                : user.getRoles().stream()
                .map(RoleEntity::getCode)
                .map(Enum::name)
                .sorted()
                .toList();
        return new UserProfileDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPictureUrl(),
                roles
        );
    }
}
