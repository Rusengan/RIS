package com.coursework.driverservice.application.handler;

import com.coursework.driverservice.application.command.CreateRoutePointCommand;
import com.coursework.driverservice.application.command.CreateTripCommand;
import com.coursework.driverservice.domain.audit.TripCreatedEvent;
import com.coursework.driverservice.infrastructure.persistence.entity.RoleCode;
import com.coursework.driverservice.infrastructure.persistence.entity.RoutePointEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.TripEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.TripStatus;
import com.coursework.driverservice.infrastructure.persistence.entity.UserEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.VehicleEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.VehicleStatus;
import com.coursework.driverservice.infrastructure.persistence.repository.TripRepository;
import com.coursework.driverservice.infrastructure.persistence.repository.UserRepository;
import com.coursework.driverservice.infrastructure.persistence.repository.VehicleRepository;
import com.coursework.driverservice.infrastructure.web.dto.TripDto;
import com.coursework.driverservice.infrastructure.web.exception.BusinessRuleException;
import com.coursework.driverservice.infrastructure.web.mapper.TripMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CreateTripCommandHandler {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final TripMapper tripMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TripDto handle(CreateTripCommand cmd) {
        UserEntity driver = userRepository.findByIdWithRoles(cmd.driverId())
                .orElseThrow(() -> new BusinessRuleException("DRIVER_NOT_FOUND"));
        boolean isDriver = driver.getRoles().stream().anyMatch(r -> r.getCode() == RoleCode.DRIVER);
        if (!isDriver) {
            throw new BusinessRuleException("USER_NOT_DRIVER");
        }

        VehicleEntity vehicle = vehicleRepository.findById(cmd.vehicleId())
                .orElseThrow(() -> new BusinessRuleException("VEHICLE_NOT_FOUND"));
        if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
            throw new BusinessRuleException("VEHICLE_NOT_ACTIVE");
        }

        UserEntity dispatcher = userRepository.findByIdWithRoles(cmd.dispatcherId())
                .orElseThrow(() -> new BusinessRuleException("DISPATCHER_NOT_FOUND"));
        boolean canDispatch = dispatcher.getRoles().stream()
                .anyMatch(r -> r.getCode() == RoleCode.DISPATCHER || r.getCode() == RoleCode.ADMIN);
        if (!canDispatch) {
            throw new BusinessRuleException("USER_NOT_DISPATCHER");
        }

        TripEntity trip = TripEntity.builder()
                .driver(driver)
                .vehicle(vehicle)
                .dispatcher(dispatcher)
                .status(TripStatus.PLANNED)
                .plannedStartAt(cmd.plannedStartAt())
                .build();

        for (CreateRoutePointCommand p : cmd.points()) {
            RoutePointEntity point = RoutePointEntity.builder()
                    .trip(trip)
                    .sequenceNo(p.sequenceNo())
                    .address(p.address())
                    .latitude(p.latitude())
                    .longitude(p.longitude())
                    .pointType(p.type())
                    .build();
            trip.getRoutePoints().add(point);
        }

        TripEntity saved = tripRepository.save(trip);
        Map<String, Object> payload = new HashMap<>();
        payload.put("driverId", saved.getDriver().getId());
        payload.put("vehicleId", saved.getVehicle().getId());
        payload.put("status", saved.getStatus().name());
        payload.put("plannedStartAt", saved.getPlannedStartAt().toString());
        eventPublisher.publishEvent(new TripCreatedEvent(cmd.dispatcherId(), saved.getId(), payload));
        return tripMapper.toDto(saved);
    }
}
