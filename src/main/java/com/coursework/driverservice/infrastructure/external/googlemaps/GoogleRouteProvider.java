package com.coursework.driverservice.infrastructure.external.googlemaps;

import com.coursework.driverservice.domain.port.out.GeoPoint;
import com.coursework.driverservice.domain.port.out.RouteCalculationRequest;
import com.coursework.driverservice.domain.port.out.RouteCalculationResult;
import com.coursework.driverservice.domain.port.out.RouteProvider;
import com.coursework.driverservice.infrastructure.web.exception.ExternalServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component("googleRouteProvider")
@RequiredArgsConstructor
@Profile("!test & !stub")
public class GoogleRouteProvider implements RouteProvider {

    private static final String FIELD_MASK = "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline";

    private final RestClient googleRoutesRestClient;
    private final GoogleMapsProperties googleMapsProperties;
    private final ObjectMapper objectMapper;

    @Override
    public RouteCalculationResult calculate(RouteCalculationRequest request) {
        String apiKey = googleMapsProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new ExternalServiceException("Google Routes API failed");
        }

        Map<String, Object> body = buildBody(request);
        try {
            String rawJson = googleRoutesRestClient.post()
                    .uri("/directions/v2:computeRoutes")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("X-Goog-Api-Key", apiKey.trim())
                    .header("X-Goog-FieldMask", FIELD_MASK)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode routes = root.path("routes");
            if (!routes.isArray() || routes.isEmpty()) {
                throw new ExternalServiceException("Google Routes API failed");
            }
            JsonNode route = routes.get(0);
            String encodedPolyline = route.path("polyline").path("encodedPolyline").asText(null);
            int distanceMeters = route.path("distanceMeters").asInt(-1);
            int durationSeconds = parseDurationSeconds(route.path("duration"));
            if (encodedPolyline == null || encodedPolyline.isBlank() || distanceMeters < 0 || durationSeconds < 0) {
                throw new ExternalServiceException("Google Routes API failed");
            }

            return new RouteCalculationResult(
                    encodedPolyline,
                    distanceMeters,
                    durationSeconds,
                    "GOOGLE",
                    Instant.now()
            );
        } catch (RestClientResponseException e) {
            throw new ExternalServiceException("Google Routes API failed", e);
        } catch (RestClientException e) {
            throw new ExternalServiceException("Google Routes API failed", e);
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException("Google Routes API failed", e);
        }
    }

    private static int parseDurationSeconds(JsonNode durationNode) {
        if (durationNode == null || durationNode.isMissingNode()) {
            return -1;
        }
        if (durationNode.isTextual()) {
            String s = durationNode.asText();
            if (s.endsWith("s")) {
                s = s.substring(0, s.length() - 1);
            }
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        if (durationNode.isObject()) {
            if (durationNode.has("seconds")) {
                return durationNode.get("seconds").asInt(-1);
            }
        }
        if (durationNode.isIntegralNumber()) {
            return durationNode.asInt();
        }
        return -1;
    }

    private static Map<String, Object> buildBody(RouteCalculationRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("origin", locationFrom(request.origin()));
        body.put("destination", locationFrom(request.destination()));
        body.put("travelMode", "DRIVE");
        body.put("routingPreference", "TRAFFIC_AWARE");
        body.put("units", "METRIC");
        List<GeoPoint> wps = request.waypoints() == null ? List.of() : request.waypoints();
        if (!wps.isEmpty()) {
            List<Map<String, Object>> intermediates = new ArrayList<>();
            for (GeoPoint p : wps) {
                intermediates.add(Map.of("location", locationFrom(p)));
            }
            body.put("intermediates", intermediates);
        }
        return body;
    }

    private static Map<String, Object> locationFrom(GeoPoint p) {
        double lat = p.latitude() == null ? 0.0 : p.latitude().doubleValue();
        double lng = p.longitude() == null ? 0.0 : p.longitude().doubleValue();
        return Map.of(
                "location",
                Map.of("latLng", Map.of("latitude", lat, "longitude", lng))
        );
    }
}
