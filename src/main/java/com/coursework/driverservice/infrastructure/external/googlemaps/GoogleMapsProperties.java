package com.coursework.driverservice.infrastructure.external.googlemaps;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.google.maps")
public class GoogleMapsProperties {

    /**
     * Google Routes / Maps API key.
     */
    private String apiKey = "";
}
