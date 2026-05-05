package com.coursework.driverservice.infrastructure.web.controller;

import com.coursework.driverservice.application.command.EndBreakCommand;
import com.coursework.driverservice.application.handler.EndBreakCommandHandler;
import com.coursework.driverservice.infrastructure.web.dto.BreakLogDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/breaks")
@RequiredArgsConstructor
public class BreakController {

    private final EndBreakCommandHandler endBreakCommandHandler;

    @PostMapping("/{id}/end")
    @PreAuthorize("hasRole('DRIVER')")
    public BreakLogDto endBreak(@PathVariable Long id) {
        Long driverId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        EndBreakCommand command = new EndBreakCommand(id, driverId);
        return endBreakCommandHandler.handle(command);
    }
}