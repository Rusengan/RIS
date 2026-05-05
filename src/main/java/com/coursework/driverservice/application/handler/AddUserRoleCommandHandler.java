package com.coursework.driverservice.application.handler;

import com.coursework.driverservice.infrastructure.persistence.entity.RoleCode;
import com.coursework.driverservice.infrastructure.persistence.entity.RoleEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.UserEntity;
import com.coursework.driverservice.infrastructure.persistence.repository.RoleRepository;
import com.coursework.driverservice.infrastructure.persistence.repository.UserRepository;
import com.coursework.driverservice.infrastructure.web.dto.UserDto;
import com.coursework.driverservice.infrastructure.web.exception.NotFoundException;
import com.coursework.driverservice.infrastructure.web.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddUserRoleCommandHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    @Transactional
    @CacheEvict(value = "userProfile", key = "#userId")
    public UserDto handle(Long userId, RoleCode roleCode) {
        UserEntity user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        RoleEntity role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new IllegalStateException("Role is not seeded: " + roleCode));

        user.getRoles().add(role);
        UserEntity saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }
}
