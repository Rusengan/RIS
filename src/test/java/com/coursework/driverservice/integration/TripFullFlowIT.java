package com.coursework.driverservice.integration;

import com.coursework.driverservice.DriverServiceApplication;
import com.coursework.driverservice.application.command.CreateTripCommand;
import com.coursework.driverservice.application.command.CreateRoutePointCommand;
import com.coursework.driverservice.domain.port.out.RouteCalculationResult;
import com.coursework.driverservice.domain.port.out.RouteProvider;
import com.coursework.driverservice.infrastructure.config.security.JwtAuthenticationFilter;
import com.coursework.driverservice.infrastructure.persistence.entity.RoleCode;
import com.coursework.driverservice.infrastructure.persistence.entity.RoleEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.RoutePointType;
import com.coursework.driverservice.infrastructure.persistence.entity.TripEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.TripStatus;
import com.coursework.driverservice.infrastructure.persistence.entity.UserEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.VehicleEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.VehicleStatus;
import com.coursework.driverservice.infrastructure.persistence.repository.RoleRepository;
import com.coursework.driverservice.infrastructure.persistence.repository.RoutePointRepository;
import com.coursework.driverservice.infrastructure.persistence.repository.TripRepository;
import com.coursework.driverservice.infrastructure.persistence.repository.UserRepository;
import com.coursework.driverservice.infrastructure.persistence.repository.VehicleRepository;
import com.coursework.driverservice.infrastructure.audit.AuditActions;
import com.coursework.driverservice.infrastructure.persistence.entity.AuditLogEntity;
import com.coursework.driverservice.infrastructure.persistence.repository.AuditLogRepository;
import com.coursework.driverservice.infrastructure.web.dto.WorkSessionDto;
import com.coursework.driverservice.infrastructure.web.dto.TripDto;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = DriverServiceApplication.class)
@Testcontainers
@ActiveProfiles({"test", "stub"})
class TripFullFlowIT {

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
    VehicleRepository vehicleRepository;

    @Autowired
    TripRepository tripRepository;

    @Autowired
    RoutePointRepository routePointRepository;

    @Autowired
    AuditLogRepository auditLogRepository;

    @MockBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    RouteProvider routeProvider;

    @BeforeEach
    void mockJwt() throws Exception {
        Mockito.doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            Long userId = Long.parseLong(request.getHeader("X-Test-User-Id"));
            String rolesHeader = request.getHeader("X-Test-Roles");
            List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesHeader.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(userId, null, authorities));
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(Mockito.any(), Mockito.any(), Mockito.any());

        when(routeProvider.calculate(any())).thenReturn(new RouteCalculationResult(
                "_p~iF~ps|U_ulLnnqC_mqNvxq`@",
                5000,
                1200,
                "STUB",
                Instant.now()
        ));
    }

    @BeforeEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void fullTripFlow_createsAuditAndTotals() throws Exception {
        RoleEntity driverRole = roleRepository.findByCode(RoleCode.DRIVER).orElseThrow();
        RoleEntity dispatcherRole = roleRepository.findByCode(RoleCode.DISPATCHER).orElseThrow();

        UserEntity driver = userRepository.save(UserEntity.builder()
                .email("trip-driver@it.local")
                .fullName("Trip Driver")
                .enabled(true)
                .roles(new HashSet<>(List.of(driverRole)))
                .build());

        UserEntity dispatcher = userRepository.save(UserEntity.builder()
                .email("trip-disp@it.local")
                .fullName("Trip Dispatcher")
                .enabled(true)
                .roles(new HashSet<>(List.of(dispatcherRole)))
                .build());

        VehicleEntity vehicle = vehicleRepository.save(VehicleEntity.builder()
                .plateNumber("IT-100")
                .brand("Test")
                .model("Van")
                .status(VehicleStatus.ACTIVE)
                .build());

        Instant planned = Instant.now().plus(2, ChronoUnit.HOURS);
        CreateTripCommand tripBody = new CreateTripCommand(
                driver.getId(),
                vehicle.getId(),
                dispatcher.getId(),
                planned,
                List.of(
                        new CreateRoutePointCommand((short) 1, "A", BigDecimal.valueOf(55.75), BigDecimal.valueOf(37.62), RoutePointType.ORIGIN),
                        new CreateRoutePointCommand((short) 2, "B", BigDecimal.valueOf(55.76), BigDecimal.valueOf(37.63), RoutePointType.WAYPOINT),
                        new CreateRoutePointCommand((short) 3, "C", BigDecimal.valueOf(55.77), BigDecimal.valueOf(37.64), RoutePointType.DESTINATION)
                )
        );

        HttpHeaders dHeaders = jsonHeaders(dispatcher.getId(), "DISPATCHER");
        ResponseEntity<TripDto> tripResp = restTemplate.exchange(
                url("/api/v1/trips"),
                HttpMethod.POST,
                new HttpEntity<>(tripBody, dHeaders),
                TripDto.class
        );
        assertThat(tripResp.getStatusCode().is2xxSuccessful()).isTrue();
        TripDto trip = tripResp.getBody();
        assertThat(trip).isNotNull();
        Long tripId = trip.id();

        HttpHeaders drvHeaders = jsonHeaders(driver.getId(), "DRIVER");
        ResponseEntity<WorkSessionDto> wsResp = restTemplate.exchange(
                url("/api/v1/work-sessions/start"),
                HttpMethod.POST,
                new HttpEntity<>(drvHeaders),
                WorkSessionDto.class
        );
        assertThat(wsResp.getStatusCode().is2xxSuccessful()).isTrue();
        WorkSessionDto workSession = wsResp.getBody();
        assertThat(workSession).isNotNull();

        restTemplate.exchange(
                url("/api/v1/trips/" + tripId + "/accept"),
                HttpMethod.POST,
                new HttpEntity<>(drvHeaders),
                TripDto.class
        );

        restTemplate.exchange(
                url("/api/v1/trips/" + tripId + "/route/calculate"),
                HttpMethod.POST,
                new HttpEntity<>(drvHeaders),
                String.class
        );

        var points = routePointRepository.findByTripIdOrderBySequenceNoAsc(tripId);
        for (var p : points) {
            restTemplate.exchange(
                    url("/api/v1/route-points/" + p.getId() + "/check-in"),
                    HttpMethod.POST,
                    new HttpEntity<>(drvHeaders),
                    String.class
            );
        }

        ResponseEntity<TripDto> completeResp = restTemplate.exchange(
                url("/api/v1/trips/" + tripId + "/complete"),
                HttpMethod.POST,
                new HttpEntity<>(drvHeaders),
                TripDto.class
        );
        assertThat(completeResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(completeResp.getBody()).isNotNull();
        assertThat(completeResp.getBody().status()).isEqualTo(TripStatus.COMPLETED);

        TripEntity persisted = tripRepository.findById(tripId).orElseThrow();
        assertThat(persisted.getTotalDistanceM()).isNotNull();
        assertThat(persisted.getTotalDistanceM()).isEqualTo(5000);

        restTemplate.exchange(
                url("/api/v1/work-sessions/" + workSession.id() + "/close"),
                HttpMethod.POST,
                new HttpEntity<>(drvHeaders),
                WorkSessionDto.class
        );

        Thread.sleep(2000);

        List<AuditLogEntity> logs = auditLogRepository.findAll();
        assertThat(logs.stream().anyMatch(l -> AuditActions.TRIP_CREATED.equals(l.getAction()))).isTrue();
        assertThat(logs.stream().anyMatch(l -> AuditActions.TRIP_COMPLETED.equals(l.getAction()))).isTrue();
    }

    private HttpHeaders jsonHeaders(Long userId, String roles) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("X-Test-User-Id", String.valueOf(userId));
        h.set("X-Test-Roles", roles);
        return h;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
