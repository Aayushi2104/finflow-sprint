package com.finflow.admin_service.client;

import com.finflow.admin_service.dto.ApplicationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "application-service",
        url = "${services.application-service.url}"
)
public interface ApplicationClient {

    @GetMapping("/applications/admin/all")
    List<ApplicationResponse> getAllApplications(
            @RequestHeader("Authorization") String authToken);

    @PutMapping("/applications/admin/{id}/status")
    void updateStatus(@PathVariable("id") Long applicationId,
                      @RequestParam("status") String status,
                      @RequestHeader("Authorization") String authToken);
}
