package com.finflow.application_service.controller;

import com.finflow.application_service.dto.ApplicationRequest;
import com.finflow.application_service.dto.ApplicationResponse;
import com.finflow.application_service.dto.StatusResponse;
import com.finflow.application_service.entity.LoanApplication.ApplicationStatus;
import com.finflow.application_service.service.ApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/applications")
public class ApplicationController {
    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<ApplicationResponse> create(@RequestBody ApplicationRequest request, Authentication auth) {
        return ResponseEntity.ok(applicationService.createApplication(request, auth.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<ApplicationResponse> update(@PathVariable Long id, @RequestBody ApplicationRequest request, Authentication auth) {
        return ResponseEntity.ok(applicationService.updateApplication(id, request, auth.getName()));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<ApplicationResponse> submit(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(
                applicationService.submitApplication(id, auth.getName())
        );
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications(Authentication auth) {
        return ResponseEntity.ok(
                applicationService.getMyApplications(auth.getName())
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<ApplicationResponse> getOne(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(
                applicationService.getApplication(id, auth.getName())
        );
    }

    @GetMapping("/{id}/status")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<StatusResponse> getStatus(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(
                applicationService.getStatus(id, auth.getName())
        );
    }



    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ApplicationResponse>> getAllApplications() {
        return ResponseEntity.ok(applicationService.getAllApplications());
    }

    @PutMapping("/admin/{id}/status")

    public ResponseEntity<ApplicationResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam ApplicationStatus status) {
        return ResponseEntity.ok(applicationService.updateStatus(id, status));
    }
}

