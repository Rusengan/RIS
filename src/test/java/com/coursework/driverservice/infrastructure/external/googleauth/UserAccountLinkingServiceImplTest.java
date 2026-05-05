package com.coursework.driverservice.infrastructure.external.googleauth;

import com.coursework.driverservice.infrastructure.persistence.entity.RoleCode;
import com.coursework.driverservice.infrastructure.persistence.entity.RoleEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.UserEntity;
import com.coursework.driverservice.infrastructure.persistence.repository.RoleRepository;
import com.coursework.driverservice.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountLinkingServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserAccountLinkingServiceImpl service;

    @Test
    void findOrCreate_returnsUser_whenFoundByGoogleSubject() {
        GoogleIdTokenPayload payload = samplePayload();
        UserEntity existing = UserEntity.builder()
                .id(10L)
                .email("test@example.com")
                .fullName("Existing")
                .googleSubject(payload.subject())
                .enabled(true)
                .build();
        UserEntity withRoles = UserEntity.builder()
                .id(10L)
                .email("test@example.com")
                .fullName("Existing")
                .googleSubject(payload.subject())
                .enabled(true)
                .roles(new HashSet<>())
                .build();

        when(userRepository.findByGoogleSubject(payload.subject())).thenReturn(Optional.of(existing));
        when(userRepository.findByIdWithRoles(10L)).thenReturn(Optional.of(withRoles));

        UserEntity result = service.findOrCreate(payload);

        assertThat(result).isSameAs(withRoles);
        verify(userRepository).findByGoogleSubject(payload.subject());
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository).save(existing);
        verify(userRepository).findByIdWithRoles(10L);
        verifyNoInteractions(roleRepository);
    }

    @Test
    void findOrCreate_linksGoogleSubject_whenFoundByEmailOnly() {
        GoogleIdTokenPayload payload = samplePayload();
        UserEntity byEmail = UserEntity.builder()
                .id(20L)
                .email(payload.email().trim())
                .fullName("Local Name")
                .enabled(true)
                .build();
        UserEntity withRoles = UserEntity.builder()
                .id(20L)
                .email(payload.email().trim())
                .fullName("Local Name")
                .googleSubject(payload.subject())
                .enabled(true)
                .roles(new HashSet<>())
                .build();

        when(userRepository.findByGoogleSubject(payload.subject())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(payload.email().trim())).thenReturn(Optional.of(byEmail));
        when(userRepository.save(byEmail)).thenReturn(byEmail);
        when(userRepository.findByIdWithRoles(20L)).thenReturn(Optional.of(withRoles));

        UserEntity result = service.findOrCreate(payload);

        assertThat(result).isSameAs(withRoles);
        assertThat(byEmail.getGoogleSubject()).isEqualTo(payload.subject());
        verify(userRepository).findByGoogleSubject(payload.subject());
        verify(userRepository).findByEmail(payload.email().trim());
        verify(userRepository).save(byEmail);
        verify(roleRepository, never()).findByCode(any());
    }

    @Test
    void findOrCreate_createsUserWithDriverRole_whenNoExistingMatch() {
        GoogleIdTokenPayload payload = samplePayload();
        RoleEntity driverRole = RoleEntity.builder().id(1L).code(RoleCode.DRIVER).build();

        when(userRepository.findByGoogleSubject(payload.subject())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(payload.email().trim())).thenReturn(Optional.empty());
        when(roleRepository.findByCode(RoleCode.DRIVER)).thenReturn(Optional.of(driverRole));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity u = invocation.getArgument(0);
            return UserEntity.builder()
                    .id(99L)
                    .email(u.getEmail())
                    .fullName(u.getFullName())
                    .googleSubject(u.getGoogleSubject())
                    .pictureUrl(u.getPictureUrl())
                    .enabled(u.getEnabled())
                    .roles(new HashSet<>(u.getRoles()))
                    .build();
        });

        UserEntity withRoles = UserEntity.builder()
                .id(99L)
                .email(payload.email().trim())
                .fullName(payload.name())
                .googleSubject(payload.subject())
                .pictureUrl(payload.picture())
                .enabled(true)
                .roles(new HashSet<>(Set.of(driverRole)))
                .build();
        when(userRepository.findByIdWithRoles(99L)).thenReturn(Optional.of(withRoles));

        UserEntity result = service.findOrCreate(payload);

        assertThat(result).isSameAs(withRoles);
        verify(roleRepository).findByCode(RoleCode.DRIVER);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        UserEntity created = captor.getValue();
        assertThat(created.getId()).isNull();
        assertThat(created.getEmail()).isEqualTo(payload.email().trim());
        assertThat(created.getGoogleSubject()).isEqualTo(payload.subject());
        assertThat(created.getRoles()).containsExactly(driverRole);
    }

    private static GoogleIdTokenPayload samplePayload() {
        return new GoogleIdTokenPayload(
                "google-sub-1",
                "test@example.com",
                true,
                "Test User",
                "https://picture.example/img.png",
                "aud",
                "iss",
                "nonce"
        );
    }
}
