package com.coursework.driverservice.infrastructure.external.googlemaps;

import com.coursework.driverservice.domain.port.out.RouteCalculationRequest;
import com.coursework.driverservice.domain.port.out.RouteCalculationResult;
import com.coursework.driverservice.domain.port.out.RouteProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

@Component
@Primary
@Profile("!test & !stub")
public class CachingRouteProviderDecorator implements RouteProvider {

    private static final Duration TTL = Duration.ofHours(24);

    private final RouteProvider delegate;
    private final RedisTemplate<String, RouteCalculationResult> routeCalculationRedisTemplate;
    private final ObjectMapper objectMapper;

    public CachingRouteProviderDecorator(
            @Qualifier("googleRouteProvider") RouteProvider delegate,
            RedisTemplate<String, RouteCalculationResult> routeCalculationRedisTemplate,
            ObjectMapper objectMapper
    ) {
        this.delegate = delegate;
        this.routeCalculationRedisTemplate = routeCalculationRedisTemplate;
        this.objectMapper = objectMapper.copy().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    @Override
    public RouteCalculationResult calculate(RouteCalculationRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(json.getBytes(StandardCharsets.UTF_8));
            String hash = HexFormat.of().formatHex(digest);
            String key = "route:hash:" + hash;

            RouteCalculationResult cached = routeCalculationRedisTemplate.opsForValue().get(key);
            if (cached != null) {
                return cached;
            }

            RouteCalculationResult computed = delegate.calculate(request);
            routeCalculationRedisTemplate.opsForValue().set(key, computed, TTL);
            return computed;
        } catch (Exception e) {
            throw new IllegalStateException("Route cache failure", e);
        }
    }
}
