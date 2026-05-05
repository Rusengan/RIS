package com.coursework.driverservice.integration;

import com.coursework.driverservice.DriverServiceApplication;
import com.coursework.driverservice.infrastructure.config.security.JwtAuthenticationFilter;
import com.coursework.driverservice.infrastructure.persistence.entity.BreakLogEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.RoleCode;
import com.coursework.driverservice.infrastructure.persistence.entity.RoleEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.UserEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.WorkSessionEntity;
import com.coursework.driverservice.infrastructure.persistence.repository.BreakLogRepository;
import com.coursework.driverservice.infrastructure.persistence.repository.RoleRepository;
import com.coursework.driverservice.infrastructure.persistence.repository.UserRepository;
import com.coursework.driverservice.infrastructure.persistence.repository.WorkSessionRepository;
import com.coursework.driverservice.infrastructure.persistence.entity.BreakType;
import com.coursework.driverservice.infrastructure.web.dto.BreakLogDto;
import com.coursework.driverservice.infrastructure.web.dto.StartBreakRequest;
import com.coursework.driverservice.infrastructure.web.dto.WorkSessionDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = DriverServiceApplication.class)
@Testcontainers
@ActiveProfiles({"test", "stub"})
class WorkSessionFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        r.add("spring.data.redis.host", redis::getHost);
        r.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    WorkSessionRepository workSessionRepository;

    @Autowired
    BreakLogRepository breakLogRepository;

    @MockBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void mockJwtFilter() throws Exception {
        Mockito.doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            String raw = request.getHeader("X-Test-User-Id");
            Long userId = raw != null ? Long.parseLong(raw) : 1L;
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_DRIVER"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @BeforeEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void workSessionFlow_calculatesTotalsAndBreakDuration() {
        RoleEntity driverRole = roleRepository.findByCode(RoleCode.DRIVER).orElseThrow();
        UserEntity driver = userRepository.save(UserEntity.builder()
                .email("driver-flow@test.local")
                .fullName("Flow Driver")
                .enabled(true)
                .roles(new HashSet<>(List.of(driverRole)))
                .build());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Test-User-Id", String.valueOf(driver.getId()));

        ResponseEntity<WorkSessionDto> startResp = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/work-sessions/start",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                WorkSessionDto.class
        );
        assertThat(startResp.getStatusCode().is2xxSuccessful()).isTrue();
        WorkSessionDto session = startResp.getBody();
        assertThat(session).isNotNull();

        StartBreakRequest breakReq = new StartBreakRequest(BreakType.SHORT);
        HttpEntity<StartBreakRequest> breakEntity = new HttpEntity<>(breakReq, headers);
        ResponseEntity<BreakLogDto> breakStartResp = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/work-sessions/" + session.id() + "/breaks/start",
                HttpMethod.POST,
                breakEntity,
                BreakLogDto.class
        );
        assertThat(breakStartResp.getStatusCode().is2xxSuccessful()).isTrue();
        BreakLogDto breakLog = breakStartResp.getBody();
        assertThat(breakLog).isNotNull();

        Optional<BreakLogEntity> active = breakLogRepository.findById(breakLog.id());
        assertThat(active).isPresent();
        BreakLogEntity bl = active.get();
        bl.setStartedAt(Instant.now().minusSeconds(120));
        breakLogRepository.save(bl);

        ResponseEntity<BreakLogDto> breakEndResp = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/breaks/" + breakLog.id() + "/end",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                BreakLogDto.class
        );
        assertThat(breakEndResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(breakEndResp.getBody()).isNotNull();
        assertThat(breakEndResp.getBody().durationMinutes()).isNotNull();
        assertThat(breakEndResp.getBody().durationMinutes()).isGreaterThan(0);

        ResponseEntity<WorkSessionDto> closeResp = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/work-sessions/" + session.id() + "/close",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                WorkSessionDto.class
        );
        assertThat(closeResp.getStatusCode().is2xxSuccessful()).isTrue();
        WorkSessionDto closed = closeResp.getBody();
        assertThat(closed).isNotNull();
        assertThat(closed.totalMinutes()).isNotNull();

        WorkSessionEntity persisted = workSessionRepository.findById(session.id()).orElseThrow();
        assertThat(persisted.getTotalMinutes()).isNotNull();
        assertThat(persisted.getBreaks())
                .anyMatch(b -> b.getDurationMinutes() != null && b.getDurationMinutes() > 0);
    }
}
