package com.coursework.driverservice.infrastructure.config.security;

import com.coursework.driverservice.infrastructure.security.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String rawToken = header.substring(7).trim();
        if (rawToken.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            jwtService.parse(rawToken).ifPresent(claims -> {
                if (JwtService.TOKEN_TYPE_REFRESH.equals(
                        String.valueOf(claims.get(JwtService.CLAIM_TOKEN_TYPE)))) {
                    log.debug("Refresh token used for access, skipping");
                    return;
                }
                Long userId = Long.valueOf(claims.getSubject());
                List<SimpleGrantedAuthority> authorities = mapRolesToAuthorities(claims);
                log.info("JWT authenticated user {} with authorities: {}", userId, authorities);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        } catch (RuntimeException e) {
            log.warn("Failed to parse JWT token: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private static List<SimpleGrantedAuthority> mapRolesToAuthorities(Claims claims) {
        Object raw = claims.get("roles");
        List<String> codes = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    codes.add(item.toString());
                }
            }
        }
        return codes.stream()
                .map(code -> code.startsWith("ROLE_") ? code : "ROLE_" + code)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}
