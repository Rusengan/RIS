package com.coursework.driverservice.application.handler;

import com.coursework.driverservice.application.command.CreateUserCommand;
import com.coursework.driverservice.domain.audit.UserCreatedEvent;
import com.coursework.driverservice.infrastructure.persistence.entity.RoleCode;
import com.coursework.driverservice.infrastructure.persistence.entity.RoleEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.UserEntity;
import com.coursework.driverservice.infrastructure.persistence.repository.RoleRepository;
import com.coursework.driverservice.infrastructure.persistence.repository.UserRepository;
import com.coursework.driverservice.infrastructure.web.dto.UserDto;
import com.coursework.driverservice.infrastructure.web.exception.BusinessRuleException;
import com.coursework.driverservice.infrastructure.web.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CreateUserCommandHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UserDto handle(CreateUserCommand cmd) {
        CreateUserCommand normalized = new CreateUserCommand(
                cmd.email().trim(),
                cmd.fullName().trim(),
                cmd.roles()
        );

        if (userRepository.existsByEmail(normalized.email())) {
            throw new BusinessRuleException("EMAIL_ALREADY_EXISTS");
        }

        Set<RoleCode> codes = Optional.ofNullable(normalized.roles()).orElse(Set.of());
        Set<RoleEntity> roleEntities = new HashSet<>();
        for (RoleCode code : codes) {
            RoleEntity role = roleRepository.findByCode(code)
                    .orElseThrow(() -> new IllegalStateException("Role is not seeded: " + code));
            roleEntities.add(role);
        }

        UserEntity entity = userMapper.toEntity(normalized);
        entity.setRoles(roleEntities);

        UserEntity saved = userRepository.save(entity);
        Long actorId = null;
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long id) {
            actorId = id;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", saved.getEmail());
        payload.put("fullName", saved.getFullName());
        payload.put("roles", codes.stream().map(RoleCode::name).toList());
        eventPublisher.publishEvent(new UserCreatedEvent(actorId, saved.getId(), payload));
        return userMapper.toDto(saved);
    }
}
