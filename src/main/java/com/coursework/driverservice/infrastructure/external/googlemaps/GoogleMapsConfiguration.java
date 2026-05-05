package com.coursework.driverservice.infrastructure.external.googlemaps;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GoogleMapsProperties.class)
public class GoogleMapsConfiguration {

    @Bean
    public RestClient googleRoutesRestClient(RestClient.Builder restClientBuilder) {
        return restClientBuilder
                .baseUrl("https://routes.googleapis.com")
                .build();
    }
}
