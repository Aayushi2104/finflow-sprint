package com.finflow.document_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final TokenClaimsService tokenClaimsService;
    private final RequestAuthenticationFactory authenticationFactory;

    public JwtAuthFilter(TokenClaimsService tokenClaimsService,
                         RequestAuthenticationFactory authenticationFactory) {
        this.tokenClaimsService = tokenClaimsService;
        this.authenticationFactory = authenticationFactory;
    }

    @Override
    protected  void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException{
        String authHeader =request.getHeader("Authorization");

        if(authHeader !=null && authHeader.startsWith("Bearer ")){
            String token =authHeader.substring(7);
            if(tokenClaimsService.isTokenValid(token)){
                TokenClaims claims = tokenClaimsService.extractClaims(token);
                SecurityContextHolder.getContext()
                        .setAuthentication(authenticationFactory.create(claims));
            }
        }
        filterChain.doFilter(request,response);
    }
}


