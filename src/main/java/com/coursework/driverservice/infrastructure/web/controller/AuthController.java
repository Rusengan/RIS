package com.coursework.driverservice.infrastructure.web.controller;

import com.coursework.driverservice.infrastructure.external.googleauth.GoogleIdTokenPayload;
import com.coursework.driverservice.infrastructure.external.googleauth.GoogleOAuthService;
import com.coursework.driverservice.infrastructure.external.googleauth.GoogleTokenResponse;
import com.coursework.driverservice.application.service.UserAccountLinkingService;
import com.coursework.driverservice.infrastructure.persistence.entity.UserEntity;
import com.coursework.driverservice.infrastructure.persistence.repository.UserRepository;
import com.coursework.driverservice.infrastructure.security.JwtService;
import com.coursework.driverservice.infrastructure.web.dto.AuthLoginResponse;
import com.coursework.driverservice.infrastructure.web.dto.RefreshTokenRequest;
import com.coursework.driverservice.infrastructure.web.dto.RefreshTokenResponse;
import com.coursework.driverservice.infrastructure.web.dto.UserProfileDto;
import com.coursework.driverservice.infrastructure.web.exception.BusinessRuleException;
import com.coursework.driverservice.infrastructure.web.mapper.UserProfileMapper;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REDIS_STATE_PREFIX = "oauth:state:";
    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    private final GoogleOAuthService googleOAuthService;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserAccountLinkingService userAccountLinkingService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserProfileMapper userProfileMapper;

    @GetMapping("/oauth2/google")
    public ResponseEntity<Void> startGoogleOAuth() {
        String state = randomUrlToken();
        String nonce = randomUrlToken();
        String key = REDIS_STATE_PREFIX + state;
        stringRedisTemplate.opsForValue().set(key, nonce, STATE_TTL);

        String location = googleOAuthService.buildAuthorizationUrl(state, nonce);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(location))
                .build();
    }

    @GetMapping("/oauth2/callback")
    public ResponseEntity<Void> googleOAuthCallback(
            @RequestParam String code,
            @RequestParam String state
    ) throws GeneralSecurityException, IOException {
        String key = REDIS_STATE_PREFIX + state;
        String storedNonce = stringRedisTemplate.opsForValue().get(key);
        if (storedNonce == null) {
            throw new BusinessRuleException("Invalid or expired OAuth state");
        }
        stringRedisTemplate.delete(key);

        GoogleTokenResponse tokens = googleOAuthService.exchangeCodeForTokens(code);
        if (tokens.idToken() == null || tokens.idToken().isBlank()) {
            throw new BusinessRuleException("Google token response has no id_token");
        }

        GoogleIdTokenPayload payload = googleOAuthService.verifyIdToken(tokens.idToken());
        if (payload.nonce() == null || !Objects.equals(payload.nonce(), storedNonce)) {
            throw new BusinessRuleException("Invalid OAuth nonce");
        }

        UserEntity user = userAccountLinkingService.findOrCreate(payload);
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        String redirectUrl = UriComponentsBuilder.fromHttpUrl(frontendUrl)
                .path("/auth/callback")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build()
                .toUriString();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest body) {
        Claims claims = jwtService.parseRefreshToken(body.refreshToken())
                .orElseThrow(() -> new BusinessRuleException("Invalid refresh token"));

        Long userId;
        try {
            userId = Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new BusinessRuleException("Invalid refresh token subject");
        }
        UserEntity user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new BusinessRuleException("User no longer exists"));

        String accessToken = jwtService.generateAccessToken(user);
        return ResponseEntity.ok(new RefreshTokenResponse(accessToken));
    }

    private static String randomUrlToken() {
        byte[] buf = new byte[32];
        SECURE_RANDOM.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
