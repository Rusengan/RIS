package com.coursework.driverservice.application.handler;

import com.coursework.driverservice.infrastructure.persistence.entity.TripStatus;
import com.coursework.driverservice.infrastructure.persistence.entity.VehicleEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.VehicleStatus;
import com.coursework.driverservice.infrastructure.persistence.repository.TripRepository;
import com.coursework.driverservice.infrastructure.persistence.repository.VehicleRepository;
import com.coursework.driverservice.infrastructure.web.exception.BusinessRuleException;
import com.coursework.driverservice.infrastructure.web.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeleteVehicleCommandHandler {

    private static final List<TripStatus> UNFINISHED_TRIP_STATUSES = List.of(
            TripStatus.PLANNED,
            TripStatus.IN_PROGRESS
    );

    private final VehicleRepository vehicleRepository;
    private final TripRepository tripRepository;

    @Transactional
    public void handle(Long id) {
        VehicleEntity vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vehicle not found: " + id));

        if (vehicle.getStatus() == VehicleStatus.ACTIVE
                && tripRepository.existsByVehicleAndStatusIn(vehicle, UNFINISHED_TRIP_STATUSES)) {
            throw new BusinessRuleException("VEHICLE_HAS_UNFINISHED_TRIPS");
        }

        vehicleRepository.delete(vehicle);
    }
}
