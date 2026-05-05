package com.coursework.driverservice.application.handler;

import com.coursework.driverservice.application.command.UpdateUserCommand;
import com.coursework.driverservice.infrastructure.persistence.entity.UserEntity;
import com.coursework.driverservice.infrastructure.persistence.repository.UserRepository;
import com.coursework.driverservice.infrastructure.web.dto.UserDto;
import com.coursework.driverservice.infrastructure.web.exception.BusinessRuleException;
import com.coursework.driverservice.infrastructure.web.exception.NotFoundException;
import com.coursework.driverservice.infrastructure.web.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateUserCommandHandler {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    @CacheEvict(value = "userProfile", key = "#id")
    public UserDto handle(Long id, UpdateUserCommand cmd) {
        UserEntity user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));

        String email = cmd.email().trim();
        if (!email.equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmailAndIdNot(email, id)) {
            throw new BusinessRuleException("EMAIL_ALREADY_EXISTS");
        }

        user.setEmail(email);
        user.setFullName(cmd.fullName().trim());
        user.setEnabled(cmd.enabled());
        user.setPictureUrl(cmd.pictureUrl());

        UserEntity saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }
}
