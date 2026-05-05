package com.coursework.driverservice.application.handler;

import com.coursework.driverservice.application.command.CreateVehicleCommand;
import com.coursework.driverservice.infrastructure.persistence.entity.VehicleEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.VehicleStatus;
import com.coursework.driverservice.infrastructure.persistence.repository.VehicleRepository;
import com.coursework.driverservice.infrastructure.web.dto.VehicleDto;
import com.coursework.driverservice.infrastructure.web.exception.BusinessRuleException;
import com.coursework.driverservice.infrastructure.web.mapper.VehicleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CreateVehicleCommandHandler {

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;

    @Transactional
    public VehicleDto handle(CreateVehicleCommand cmd) {
        String plate = cmd.plateNumber().trim();
        if (vehicleRepository.findByPlateNumber(plate).isPresent()) {
            throw new BusinessRuleException("PLATE_ALREADY_EXISTS");
        }

        VehicleEntity entity = vehicleMapper.toEntity(normalize(cmd, plate));
        entity.setStatus(Optional.ofNullable(cmd.status()).orElse(VehicleStatus.ACTIVE));

        VehicleEntity saved = vehicleRepository.save(entity);
        return vehicleMapper.toDto(saved);
    }

    private static CreateVehicleCommand normalize(CreateVehicleCommand cmd, String trimmedPlate) {
        return new CreateVehicleCommand(
                trimmedPlate,
                cmd.brand().trim(),
                cmd.model().trim(),
                cmd.capacityKg(),
                cmd.status()
        );
    }
}
