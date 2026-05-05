package com.coursework.driverservice.infrastructure.external.googleauth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

/**
 * Stable view of a verified Google ID token (see {@link GoogleIdToken#getPayload()}).
 */
public record GoogleIdTokenPayload(
        String subject,
        String email,
        Boolean emailVerified,
        String name,
        String picture,
        String audience,
        String issuer,
        String nonce
) {

    public static GoogleIdTokenPayload from(GoogleIdToken.Payload payload) {
        return new GoogleIdTokenPayload(
                payload.getSubject(),
                payload.getEmail(),
                payload.getEmailVerified(),
                stringClaim(payload, "name"),
                stringClaim(payload, "picture"),
                audienceAsString(payload.getAudience()),
                payload.getIssuer(),
                stringClaim(payload, "nonce")
        );
    }

    private static String stringClaim(GoogleIdToken.Payload payload, String name) {
        Object v = payload.get(name);
        return v == null ? null : v.toString();
    }

    private static String audienceAsString(Object audience) {
        if (audience == null) {
            return null;
        }
        if (audience instanceof Iterable<?> iterable) {
            StringBuilder sb = new StringBuilder();
            for (Object o : iterable) {
                if (o != null) {
                    if (!sb.isEmpty()) {
                        sb.append(',');
                    }
                    sb.append(o);
                }
            }
            return sb.isEmpty() ? null : sb.toString();
        }
        return audience.toString();
    }
}
