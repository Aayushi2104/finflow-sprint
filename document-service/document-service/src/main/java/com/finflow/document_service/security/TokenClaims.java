package com.finflow.document_service.security;

public record TokenClaims(String email, String role) {
}
