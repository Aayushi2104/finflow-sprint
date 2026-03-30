package com.finflow.admin_service.dto;

public record AdminStatusResponse(
        String service,
        String status,
        String applicationServiceUrl,
        String authServiceUrl
) {
}
