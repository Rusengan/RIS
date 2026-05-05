package com.coursework.driverservice.infrastructure.web.controller;

import com.coursework.driverservice.application.command.CheckInRoutePointCommand;
import com.coursework.driverservice.application.handler.CheckInRoutePointCommandHandler;
import com.coursework.driverservice.infrastructure.web.dto.RoutePointDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/route-points")
@RequiredArgsConstructor
public class RoutePointController {

    private final CheckInRoutePointCommandHandler checkInRoutePointCommandHandler;

    @PostMapping("/{id}/check-in")
    @PreAuthorize("hasRole('DRIVER')")
    public RoutePointDto checkIn(@PathVariable Long id) {
        Long driverId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return checkInRoutePointCommandHandler.handle(new CheckInRoutePointCommand(id, driverId));
    }
}
