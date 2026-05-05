package com.coursework.driverservice.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "trips")
@Table(name = "trips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private UserEntity driver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private VehicleEntity vehicle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dispatcher_id", nullable = false)
    private UserEntity dispatcher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_session_id")
    private WorkSessionEntity workSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TripStatus status;

    @Column(name = "planned_start_at", nullable = false)
    private Instant plannedStartAt;

    @Column(name = "actual_start_at")
    private Instant actualStartAt;

    @Column(name = "actual_end_at")
    private Instant actualEndAt;

    @Column(name = "total_distance_m")
    private Integer totalDistanceM;

    @Column(name = "total_duration_s")
    private Integer totalDurationS;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @OneToOne(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private RouteEntity route;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNo ASC")
    @Builder.Default
    private List<RoutePointEntity> routePoints = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
