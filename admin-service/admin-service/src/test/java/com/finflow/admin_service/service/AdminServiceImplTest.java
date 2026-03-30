package com.finflow.admin_service.service;

import com.finflow.admin_service.dto.ApplicationResponse;
import com.finflow.admin_service.dto.DecisionRequest;
import com.finflow.admin_service.dto.DecisionResponse;
import com.finflow.admin_service.dto.ReportResponse;
import com.finflow.admin_service.dto.UserResponse;
import com.finflow.admin_service.entity.Decision;
import com.finflow.admin_service.exception.ApiException;
import com.finflow.admin_service.repository.DecisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private DecisionRepository decisionRepository;

    @Mock
    private AdminDownstreamService adminDownstreamService;

    @InjectMocks
    private AdminServiceImpl adminService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminDownstreamService, "applicationServiceUrl", "http://application-service");
        ReflectionTestUtils.setField(adminDownstreamService, "authServiceUrl", "http://auth-service");
    }

    @Test
    void getAllApplications_ShouldReturnApplicationsFromDownstreamService() {
        ApplicationResponse application = buildApplication(1L, "user@finflow.com", "SUBMITTED", new BigDecimal("500000"));

        when(adminDownstreamService.getAllApplications("Bearer token")).thenReturn(List.of(application));

        List<ApplicationResponse> responses = adminService.getAllApplications("Bearer token");

        assertEquals(1, responses.size());
        assertEquals("user@finflow.com", responses.get(0).getApplicantEmail());
    }

    @Test
    void getAllApplications_ShouldThrowServiceUnavailable_WhenDownstreamIsUnreachable() {
        when(adminDownstreamService.getAllApplications("Bearer token"))
                .thenThrow(new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                        "Application service is unreachable at http://application-service"));

        ApiException exception = assertThrows(ApiException.class,
                () -> adminService.getAllApplications("Bearer token"));

        assertEquals("Application service is unreachable at http://application-service", exception.getMessage());
    }

    @Test
    void makeDecision_ShouldPersistDecisionAndUpdateStatuses() {
        DecisionRequest request = new DecisionRequest();
        request.setDecision(Decision.DecisionType.APPROVED);
        request.setRemarks("Eligible applicant");

        Decision decision = Decision.builder()
                .applicationId(10L)
                .applicantEmail("user@finflow.com")
                .decision(Decision.DecisionType.APPROVED)
                .remarks("Eligible applicant")
                .decidedBy("admin@finflow.com")
                .build();

        when(decisionRepository.findByApplicationId(10L)).thenReturn(Optional.empty());
        when(adminDownstreamService.getAllApplications("Bearer token"))
                .thenReturn(List.of(buildApplication(10L, "user@finflow.com", "SUBMITTED", new BigDecimal("500000"))));
        when(decisionRepository.save(any(Decision.class))).thenReturn(decision);

        DecisionResponse response = adminService.makeDecision(10L, request, "admin@finflow.com", "Bearer token");

        assertEquals(10L, response.getApplicationId());
        assertEquals(Decision.DecisionType.APPROVED, response.getDecision());
        verify(decisionRepository).save(any(Decision.class));
        verify(adminDownstreamService).updateApplicationStatus(10L, "UNDER_REVIEW", "Bearer token");
        verify(adminDownstreamService).updateApplicationStatus(10L, "APPROVED", "Bearer token");
    }

    @Test
    void makeDecision_ShouldThrowConflict_WhenDecisionAlreadyExists() {
        when(decisionRepository.findByApplicationId(11L)).thenReturn(Optional.of(Decision.builder()
                .applicationId(11L)
                .applicantEmail("user@finflow.com")
                .decision(Decision.DecisionType.REJECTED)
                .remarks("done")
                .decidedBy("admin@finflow.com")
                .build()));

        DecisionRequest request = new DecisionRequest();
        request.setDecision(Decision.DecisionType.REJECTED);

        ApiException exception = assertThrows(ApiException.class,
                () -> adminService.makeDecision(11L, request, "admin@finflow.com", "Bearer token"));

        assertEquals("Decision already made for application: 11", exception.getMessage());
    }

    @Test
    void getAllUsers_ShouldReturnUsersFromAuthService() {
        UserResponse user = new UserResponse();
        user.setEmail("user@finflow.com");

        when(adminDownstreamService.getAllUsers("Bearer token")).thenReturn(List.of(user));

        List<UserResponse> users = adminService.getAllUsers("Bearer token");

        assertEquals(1, users.size());
        assertEquals("user@finflow.com", users.get(0).getEmail());
    }

    @Test
    void generateReport_ShouldAggregateApplicationMetrics() {
        ApplicationResponse approved = buildApplication(20L, "u1@finflow.com", "APPROVED", new BigDecimal("100000"));
        ApplicationResponse rejected = buildApplication(21L, "u2@finflow.com", "REJECTED", new BigDecimal("50000"));
        ApplicationResponse pending = buildApplication(22L, "u3@finflow.com", "SUBMITTED", new BigDecimal("75000"));

        when(adminDownstreamService.getAllApplications("Bearer token"))
                .thenReturn(List.of(approved, rejected, pending));

        ReportResponse report = adminService.generateReport("Bearer token");

        assertEquals(3, report.getTotalApplications());
        assertEquals(1, report.getTotalApproved());
        assertEquals(1, report.getTotalRejected());
        assertEquals("33.3%", report.getApprovalRate());
        assertEquals(new BigDecimal("225000"), report.getTotalLoanAmountRequested());
        assertNotNull(report.getGeneratedAt());
    }

    @Test
    void getDailyReport_ShouldReturnDailyMetrics() {
        LocalDate date = LocalDate.now();
        ApplicationResponse approved = buildApplication(30L, "u1@finflow.com", "APPROVED", new BigDecimal("100000"));
        approved.setCreatedAt(date.atStartOfDay());
        ApplicationResponse rejected = buildApplication(31L, "u2@finflow.com", "REJECTED", new BigDecimal("50000"));
        rejected.setCreatedAt(date.atTime(10, 0));

        when(adminDownstreamService.getAllApplications("Bearer token"))
                .thenReturn(List.of(approved, rejected));

        Map<String, Object> report = adminService.getDailyReport("Bearer token", date);

        assertEquals(date.toString(), report.get("date"));
        assertEquals(2L, report.get("totalApplications"));
        assertEquals("50.0%", report.get("approvalRate"));
    }

    private ApplicationResponse buildApplication(Long id, String email, String status, BigDecimal amount) {
        ApplicationResponse response = new ApplicationResponse();
        response.setId(id);
        response.setApplicantEmail(email);
        response.setStatus(status);
        response.setLoanAmount(amount);
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }
}
