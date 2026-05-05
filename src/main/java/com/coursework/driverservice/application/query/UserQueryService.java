package com.coursework.driverservice.application.query;

import com.coursework.driverservice.infrastructure.persistence.entity.RoleCode;
import com.coursework.driverservice.infrastructure.persistence.entity.UserEntity;
import com.coursework.driverservice.infrastructure.persistence.repository.UserRepository;
import com.coursework.driverservice.infrastructure.persistence.spec.UserSpecifications;
import com.coursework.driverservice.infrastructure.web.dto.UserDto;
import com.coursework.driverservice.infrastructure.web.dto.UserProfileDto;
import com.coursework.driverservice.infrastructure.web.exception.NotFoundException;
import com.coursework.driverservice.infrastructure.web.mapper.UserMapper;
import com.coursework.driverservice.infrastructure.web.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;

    @Transactional(readOnly = true)
    @Cacheable(value = "userProfile", key = "#userId")
    public UserProfileDto getProfile(Long userId) {
        UserEntity user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        return userProfileMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public Page<UserDto> search(RoleCode role, Boolean enabled, Pageable pageable) {
        Specification<UserEntity> spec = Specification
                .where(UserSpecifications.hasRole(role))
                .and(UserSpecifications.isEnabled(enabled));
        return userRepository.findAll(spec, pageable).map(userMapper::toDto);
    }
}
