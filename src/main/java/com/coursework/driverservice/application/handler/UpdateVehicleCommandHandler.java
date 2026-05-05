package com.coursework.driverservice.application.handler;

import com.coursework.driverservice.application.command.UpdateVehicleCommand;
import com.coursework.driverservice.infrastructure.persistence.entity.VehicleEntity;
import com.coursework.driverservice.infrastructure.persistence.repository.VehicleRepository;
import com.coursework.driverservice.infrastructure.web.dto.VehicleDto;
import com.coursework.driverservice.infrastructure.web.exception.BusinessRuleException;
import com.coursework.driverservice.infrastructure.web.exception.NotFoundException;
import com.coursework.driverservice.infrastructure.web.mapper.VehicleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateVehicleCommandHandler {

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;

    @Transactional
    public VehicleDto handle(Long id, UpdateVehicleCommand cmd) {
        VehicleEntity entity = vehicleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vehicle not found: " + id));

        String plate = cmd.plateNumber().trim();
        if (!plate.equalsIgnoreCase(entity.getPlateNumber())
                && vehicleRepository.existsByPlateNumberAndIdNot(plate, id)) {
            throw new BusinessRuleException("PLATE_ALREADY_EXISTS");
        }

        vehicleMapper.updateEntity(
                new UpdateVehicleCommand(
                        plate,
                        cmd.brand().trim(),
                        cmd.model().trim(),
                        cmd.capacityKg(),
                        cmd.status()
                ),
                entity
        );

        return vehicleMapper.toDto(vehicleRepository.save(entity));
    }
}
