package com.finflow.document_service.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RequestAuthenticationFactory {

    public UsernamePasswordAuthenticationToken create(TokenClaims claims) {
        return new UsernamePasswordAuthenticationToken(
                claims.email(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + claims.role()))
        );
    }
}
