package com.coursework.driverservice.infrastructure.security;

import com.coursework.driverservice.infrastructure.config.JwtProperties;
import com.coursework.driverservice.infrastructure.persistence.entity.RoleEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JwtService {

    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";

    private final JwtProperties jwtProperties;

    @Transactional(readOnly = true)
    public String generateAccessToken(UserEntity user) {
        return buildToken(user, jwtProperties.getAccessTtl(), TOKEN_TYPE_ACCESS);
    }

    @Transactional(readOnly = true)
    public String generateRefreshToken(UserEntity user) {
        return buildToken(user, jwtProperties.getRefreshTtl(), TOKEN_TYPE_REFRESH);
    }

    /**
     * Parses a JWT and returns claims only if it is a valid refresh token.
     */
    public Optional<Claims> parseRefreshToken(String token) {
        return parse(token).filter(c ->
                TOKEN_TYPE_REFRESH.equals(String.valueOf(c.get(CLAIM_TOKEN_TYPE))));
    }

    public Optional<Claims> parse(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .requireIssuer(jwtProperties.getIssuer())
                    .build()
                    .parseSignedClaims(token.trim())
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private String buildToken(UserEntity user, Duration ttl, String tokenType) {
        Instant now = Instant.now();
        Instant exp = now.plus(ttl);
        return Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("roles", roleCodes(user))
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    private List<String> roleCodes(UserEntity user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return List.of();
        }
        return user.getRoles().stream()
                .map(RoleEntity::getCode)
                .map(Enum::name)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private SecretKey signingKey() {
        byte[] secretBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        try {
            return Keys.hmacShaKeyFor(MessageDigest.getInstance("SHA-256").digest(secretBytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
