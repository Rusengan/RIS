package com.coursework.driverservice.application.query;

import com.coursework.driverservice.infrastructure.persistence.entity.TripEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.TripStatus;
import com.coursework.driverservice.infrastructure.persistence.repository.TripRepository;
import com.coursework.driverservice.infrastructure.persistence.spec.TripSpecifications;
import com.coursework.driverservice.infrastructure.web.dto.TripDto;
import com.coursework.driverservice.infrastructure.web.mapper.TripMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TripQueryService {

    private final TripRepository tripRepository;
    private final TripMapper tripMapper;

    @Transactional(readOnly = true)
    public Page<TripDto> search(
            TripStatus status,
            Long driverId,
            Long vehicleId,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        Specification<TripEntity> spec = Specification
                .where(TripSpecifications.hasStatus(status))
                .and(TripSpecifications.hasDriver(driverId))
                .and(TripSpecifications.hasVehicle(vehicleId))
                .and(TripSpecifications.plannedBetween(from, to));

        return tripRepository.findAll(spec, pageable).map(tripMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<TripDto> searchForDriver(Long driverId, TripStatus status, Instant from, Instant to, Pageable pageable) {
        Specification<TripEntity> spec = Specification
                .where(TripSpecifications.hasDriver(driverId))
                .and(TripSpecifications.hasStatus(status))
                .and(TripSpecifications.plannedBetween(from, to));

        return tripRepository.findAll(spec, pageable).map(tripMapper::toDto);
    }
}
