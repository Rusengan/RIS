package com.coursework.driverservice.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity(name = "routes")
@Table(name = "routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false, unique = true)
    private TripEntity trip;

    @Column(name = "encoded_polyline", nullable = false, columnDefinition = "TEXT")
    private String encodedPolyline;

    @Column(name = "total_distance_m", nullable = false)
    private Integer totalDistanceM;

    @Column(name = "total_duration_s", nullable = false)
    private Integer totalDurationS;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private RoutingEngine provider = RoutingEngine.GOOGLE;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;
}
