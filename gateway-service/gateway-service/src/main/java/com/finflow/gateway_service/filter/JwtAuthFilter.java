package com.finflow.gateway_service.filter;

import com.finflow.gateway_service.config.PublicEndpointMatcher;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final PublicEndpointMatcher publicEndpointMatcher;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.info("Incoming request: {}", path);


        if (publicEndpointMatcher.isPublic(path)) {
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
        String role = jwtUtil.extractRole(token);

        log.info("Authenticated: {} | Role: {}", email, role);

        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Email", email)
                .header("X-User-Role", role)
                .build();

        return chain.filter(exchange.mutate()
                .request(modifiedRequest).build());
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

