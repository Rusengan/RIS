package com.coursework.driverservice.infrastructure.external.googlemaps;

import com.coursework.driverservice.domain.port.out.RouteCalculationRequest;
import com.coursework.driverservice.domain.port.out.RouteCalculationResult;
import com.coursework.driverservice.domain.port.out.RouteProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Primary
@Profile({ "test", "stub" })
public class StubRouteProvider implements RouteProvider {

    @Override
    public RouteCalculationResult calculate(RouteCalculationRequest request) {
        return new RouteCalculationResult(
                "_p~iF~ps|U_ulLnnqC_mqNvxq`@",
                1000,
                600,
                "STUB",
                Instant.now()
        );
    }
}
