package com.coursework.driverservice.infrastructure.external.googlemaps;

import com.coursework.driverservice.domain.port.out.GeoPoint;
import com.coursework.driverservice.domain.port.out.RouteCalculationRequest;
import com.coursework.driverservice.domain.port.out.RouteCalculationResult;
import com.coursework.driverservice.domain.port.out.RouteProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CachingRouteProviderDecoratorTest {

    @Mock
    private RouteProvider delegate;

    @Mock
    private RedisTemplate<String, RouteCalculationResult> redisTemplate;

    @Mock
    private ValueOperations<String, RouteCalculationResult> valueOps;

    private CachingRouteProviderDecorator decorator;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        decorator = new CachingRouteProviderDecorator(delegate, redisTemplate, new ObjectMapper());
    }

    @Test
    void calculate_whenCacheMiss_callsDelegateAndStoresWithTtl() {
        GeoPoint o = new GeoPoint(BigDecimal.ONE, BigDecimal.valueOf(2));
        GeoPoint d = new GeoPoint(BigDecimal.valueOf(3), BigDecimal.valueOf(4));
        RouteCalculationRequest req = new RouteCalculationRequest(o, d, List.of());
        RouteCalculationResult computed =
                new RouteCalculationResult("poly", 10, 20, "GOOGLE", Instant.parse("2024-01-01T00:00:00Z"));

        when(valueOps.get(any())).thenReturn(null);
        when(delegate.calculate(any())).thenReturn(computed);

        RouteCalculationResult result = decorator.calculate(req);

        assertThat(result).isEqualTo(computed);
        verify(delegate, times(1)).calculate(any());

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOps).set(any(String.class), eq(computed), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofHours(24));
    }

    @Test
    void calculate_whenCacheHit_doesNotCallDelegate() {
        GeoPoint o = new GeoPoint(BigDecimal.ONE, BigDecimal.valueOf(2));
        GeoPoint d = new GeoPoint(BigDecimal.valueOf(3), BigDecimal.valueOf(4));
        RouteCalculationRequest req = new RouteCalculationRequest(o, d, List.of());
        RouteCalculationResult cached =
                new RouteCalculationResult("x", 1, 2, "GOOGLE", Instant.parse("2024-01-02T00:00:00Z"));

        when(valueOps.get(any())).thenReturn(cached);

        RouteCalculationResult result = decorator.calculate(req);

        assertThat(result).isSameAs(cached);
        verify(delegate, never()).calculate(any());
    }

    @Test
    void calculate_waypointOrderProducesDifferentCacheKeys() throws Exception {
        GeoPoint a = new GeoPoint(BigDecimal.valueOf(1.1), BigDecimal.valueOf(2.2));
        GeoPoint b = new GeoPoint(BigDecimal.valueOf(3.3), BigDecimal.valueOf(4.4));
        GeoPoint o = new GeoPoint(BigDecimal.ZERO, BigDecimal.ZERO);
        GeoPoint dest = new GeoPoint(BigDecimal.TEN, BigDecimal.TEN);

        RouteCalculationRequest req1 = new RouteCalculationRequest(o, dest, List.of(a, b));
        RouteCalculationRequest req2 = new RouteCalculationRequest(o, dest, List.of(b, a));

        ObjectMapper canonical = new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        assertThat(canonical.writeValueAsString(req1)).isNotEqualTo(canonical.writeValueAsString(req2));

        when(valueOps.get(any())).thenReturn(null);
        when(delegate.calculate(any())).thenAnswer(inv -> new RouteCalculationResult(
                "p", 1, 1, "GOOGLE", Instant.now()));

        decorator.calculate(req1);
        decorator.calculate(req2);

        verify(delegate, times(2)).calculate(any());
    }
}
