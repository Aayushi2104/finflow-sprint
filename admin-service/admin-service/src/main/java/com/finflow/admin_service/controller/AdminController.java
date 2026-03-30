package com.finflow.admin_service.controller;

import com.finflow.admin_service.dto.ApplicationResponse;
import com.finflow.admin_service.dto.DecisionRequest;
import com.finflow.admin_service.dto.DecisionResponse;
import com.finflow.admin_service.dto.ReportResponse;
import com.finflow.admin_service.dto.UserResponse;
import com.finflow.admin_service.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ── Get All Applications ──
    @GetMapping("/applications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ApplicationResponse>> getAllApplications(
            @RequestHeader("Authorization") String authToken) {
        return ResponseEntity.ok(
                adminService.getAllApplications(authToken));
    }

    // ── Make Decision ──
    @PostMapping("/applications/{id}/decision")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DecisionResponse> makeDecision(
            @PathVariable Long id,
            @Valid @RequestBody DecisionRequest request,
            Authentication auth,
            @RequestHeader("Authorization") String authToken) {
        return ResponseEntity.ok(
                adminService.makeDecision(id, request, auth.getName(), authToken));
    }

    // ── Get All Users ──
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @RequestHeader("Authorization") String authToken) {
        return ResponseEntity.ok(
                adminService.getAllUsers(authToken));
    }

    // ── Full Report ──
    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> getReport(
            @RequestHeader("Authorization") String authToken) {
        return ResponseEntity.ok(
                adminService.generateReport(authToken));
    }

    // ── Today's Report ──
    @GetMapping("/reports/today")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getTodayReport(
            @RequestHeader("Authorization") String authToken) {
        return ResponseEntity.ok(
                adminService.getDailyReport(authToken, LocalDate.now()));
    }

    // ── Specific Day Report ──
    @GetMapping("/reports/day")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDayReport(
            @RequestHeader("Authorization") String authToken,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(
                adminService.getDailyReport(authToken, date));
    }
}
