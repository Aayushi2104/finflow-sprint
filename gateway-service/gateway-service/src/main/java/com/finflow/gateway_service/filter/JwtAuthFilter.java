package com.finflow.gateway_service.filter;

import com.finflow.gateway_service.config.GatewaySecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final GatewaySecurityProperties securityProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.info("Incoming request: {}", path);


        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }


        if (!request.getHeaders().containsKey("Authorization")) {
            return sendUnauthorized(exchange, "Missing Authorization header");
        }

        String authHeader = request.getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return sendUnauthorized(exchange, "Invalid Authorization format");
        }

        String token = authHeader.substring(7);


        if (!jwtUtil.isTokenValid(token)) {
            return sendUnauthorized(exchange, "Invalid or expired token");
        }


        String email = jwtUtil.extractEmail(token);
        String role  = jwtUtil.extractRole(token);

        log.info("Authenticated: {} | Role: {}", email, role);

        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Email", email)
                .header("X-User-Role", role)
                .build();

        return chain.filter(exchange.mutate()
                .request(modifiedRequest).build());
    }

    private boolean isPublicEndpoint(String path) {
        return securityProperties.getPublicEndpoints().stream()
                .anyMatch(endpoint -> matchesPublicEndpoint(path, endpoint));
    }

    private boolean matchesPublicEndpoint(String path, String endpoint) {
        if (endpoint.endsWith("/**")) {
            String prefix = endpoint.substring(0, endpoint.length() - 3);
            return path.startsWith(prefix);
        }
        return path.equals(endpoint);
    }

    private Mono<Void> sendUnauthorized(ServerWebExchange exchange,
                                        String message) {
        log.warn("Unauthorized: {}", message);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}

