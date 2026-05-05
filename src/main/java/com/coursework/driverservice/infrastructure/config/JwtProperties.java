package com.coursework.driverservice.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties("app.jwt")
public class JwtProperties {

    @NotBlank
    private String issuer;

    @NotNull
    private Duration accessTtl;

    @NotNull
    private Duration refreshTtl;

    /**
     * HMAC secret (any length; internally derived to a 256-bit key for HS256).
     */
    @NotBlank
    private String secret;
}
