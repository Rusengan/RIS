package com.coursework.driverservice.application.query;

import com.coursework.driverservice.infrastructure.persistence.entity.VehicleEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.VehicleStatus;
import com.coursework.driverservice.infrastructure.persistence.repository.VehicleRepository;
import com.coursework.driverservice.infrastructure.persistence.spec.VehicleSpecifications;
import com.coursework.driverservice.infrastructure.web.dto.VehicleDto;
import com.coursework.driverservice.infrastructure.web.mapper.VehicleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VehicleQueryService {

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;

    @Transactional(readOnly = true)
    public Page<VehicleDto> search(VehicleStatus status, Pageable pageable) {
        Specification<VehicleEntity> spec = Specification.where(VehicleSpecifications.byStatus(status));
        return vehicleRepository.findAll(spec, pageable).map(vehicleMapper::toDto);
    }
}
