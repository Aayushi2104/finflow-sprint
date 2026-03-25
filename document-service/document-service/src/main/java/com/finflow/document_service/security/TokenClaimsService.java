package com.finflow.document_service.security;

public interface TokenClaimsService {

    boolean isTokenValid(String token);

    TokenClaims extractClaims(String token);
}
