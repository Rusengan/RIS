package com.coursework.driverservice.infrastructure.external.googleauth;

import com.coursework.driverservice.application.service.UserAccountLinkingService;
import com.coursework.driverservice.infrastructure.persistence.entity.RoleCode;
import com.coursework.driverservice.infrastructure.persistence.entity.RoleEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.UserEntity;
import com.coursework.driverservice.infrastructure.persistence.repository.RoleRepository;
import com.coursework.driverservice.infrastructure.persistence.repository.UserRepository;
import com.coursework.driverservice.infrastructure.web.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserAccountLinkingServiceImpl implements UserAccountLinkingService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public UserEntity findOrCreate(GoogleIdTokenPayload payload) {
        if (payload.email() == null || payload.email().isBlank()) {
            throw new BusinessRuleException("Google account has no email");
        }
        String email = payload.email().trim();
        String subject = payload.subject();

        Optional<UserEntity> byGoogle = userRepository.findByGoogleSubject(subject);
        if (byGoogle.isPresent()) {
            return updateProfile(byGoogle.get(), email, payload);
        }

        Optional<UserEntity> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            UserEntity user = byEmail.get();
            user.setGoogleSubject(subject);
            applyProfile(user, email, payload);
            userRepository.save(user);
            return userRepository.findByIdWithRoles(user.getId()).orElseThrow();
        }

        RoleEntity driverRole = roleRepository.findByCode(RoleCode.DRIVER)
                .orElseThrow(() -> new IllegalStateException("Role DRIVER is not seeded"));

        UserEntity created = UserEntity.builder()
                .email(email)
                .fullName(Optional.ofNullable(payload.name()).filter(n -> !n.isBlank()).orElse(email))
                .googleSubject(subject)
                .pictureUrl(payload.picture())
                .enabled(true)
                .roles(new HashSet<>(Set.of(driverRole)))
                .build();

        UserEntity saved = userRepository.save(created);
        return userRepository.findByIdWithRoles(saved.getId()).orElseThrow();
    }

    private UserEntity updateProfile(UserEntity user, String email, GoogleIdTokenPayload payload) {
        if (!Objects.equals(user.getEmail(), email)) {
            user.setEmail(email);
        }
        applyProfile(user, email, payload);
        userRepository.save(user);
        return userRepository.findByIdWithRoles(user.getId()).orElseThrow();
    }

    private static void applyProfile(UserEntity user, String email, GoogleIdTokenPayload payload) {
        if (user.getFullName() == null || user.getFullName().isBlank()) {
            user.setFullName(Optional.ofNullable(payload.name()).filter(n -> !n.isBlank()).orElse(email));
        }
        if (payload.picture() != null) {
            user.setPictureUrl(payload.picture());
        }
    }
}
