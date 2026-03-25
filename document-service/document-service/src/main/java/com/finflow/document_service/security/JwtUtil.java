package com.finflow.document_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class JwtUtil implements TokenClaimsService {

    @Value("${jwt.secret}")
    private String secret;

    private Key getSigningKey(){
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String extractEmail(String token){
        return getClaims(token).getSubject();
    }

    public String extractRole(String token){
        return getClaims(token).get("role",String.class);
    }

    @Override
    public TokenClaims extractClaims(String token) {
        return new TokenClaims(extractEmail(token), extractRole(token));
    }

    @Override
    public boolean isTokenValid(String token){
        try{
            getClaims(token);
            return true;
        }catch(JwtException |IllegalArgumentException e){
            return false;
        }
    }

    public Claims getClaims(String token){
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
