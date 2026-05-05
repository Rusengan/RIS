package com.coursework.driverservice.infrastructure.external.googleauth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

@Service
public class GoogleOAuthService {

    private static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String SCOPES = "openid email profile";

    private final GoogleOAuthProperties properties;
    private final RestClient googleTokenClient;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public GoogleOAuthService(
            GoogleOAuthProperties properties,
            GoogleIdTokenVerifier googleIdTokenVerifier,
            RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.googleTokenClient = restClientBuilder
                .baseUrl("https://oauth2.googleapis.com")
                .build();
        this.googleIdTokenVerifier = googleIdTokenVerifier;
    }

    public String buildAuthorizationUrl(String state, String nonce) {
        return UriComponentsBuilder.fromUriString(AUTH_ENDPOINT)
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", SCOPES)
                .queryParam("state", state)
                .queryParam("nonce", nonce)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
    }

    public GoogleTokenResponse exchangeCodeForTokens(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("redirect_uri", properties.getRedirectUri());

        return googleTokenClient.post()
                .uri("/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(GoogleTokenResponse.class);
    }

    public GoogleIdTokenPayload verifyIdToken(String idToken) throws GeneralSecurityException, IOException {
        GoogleIdToken token = googleIdTokenVerifier.verify(idToken);
        if (token == null) {
            throw new IllegalArgumentException("Invalid Google ID token");
        }
        return GoogleIdTokenPayload.from(token.getPayload());
    }
}
