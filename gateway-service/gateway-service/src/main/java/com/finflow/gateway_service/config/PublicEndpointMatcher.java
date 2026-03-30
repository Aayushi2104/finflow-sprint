package com.finflow.gateway_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class PublicEndpointMatcher {

    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/gateway/auth/signup",
            "/gateway/auth/login",
            "/gateway/auth/create-admin",
            "/gateway/auth/products"
    );

    private final GatewaySecurityProperties securityProperties;

    public boolean isPublic(String path) {
        return Stream.concat(
                        PUBLIC_ENDPOINTS.stream(),
                        securityProperties.getPublicEndpoints().stream()
                )
                .anyMatch(endpoint -> matches(path, endpoint));
    }

    private boolean matches(String path, String endpoint) {
        if (endpoint.endsWith("/**")) {
            String prefix = endpoint.substring(0, endpoint.length() - 3);
            return path.startsWith(prefix);
        }
        return path.equals(endpoint);
    }
}
