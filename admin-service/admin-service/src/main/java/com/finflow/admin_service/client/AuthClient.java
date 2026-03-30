package com.finflow.admin_service.client;

import com.finflow.admin_service.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(
        name = "auth-service",
        url = "${services.auth-service.url}"
)
public interface AuthClient {

    @GetMapping("/auth/admin/users")
    List<UserResponse> getAllUsers(@RequestHeader("Authorization") String authToken);
}
