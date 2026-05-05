package com.coursework.driverservice.domain.port.out;

public interface RouteProvider {

    RouteCalculationResult calculate(RouteCalculationRequest request);
}
