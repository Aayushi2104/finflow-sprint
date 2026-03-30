package com.finflow.admin_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finflow.admin_service.dto.ApplicationResponse;
import com.finflow.admin_service.dto.DecisionRequest;
import com.finflow.admin_service.dto.DecisionResponse;
import com.finflow.admin_service.dto.ReportResponse;
import com.finflow.admin_service.dto.UserResponse;
import com.finflow.admin_service.entity.Decision;
import com.finflow.admin_service.exception.GlobalExceptionHandler;
import com.finflow.admin_service.security.JwtAuthFilter;
import com.finflow.admin_service.security.JwtUtil;
import com.finflow.admin_service.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({AdminControllerTest.TestSecurityConfig.class, GlobalExceptionHandler.class})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    @WithMockUser(username = "admin@finflow.com", roles = "ADMIN")
    void getAllApplications_ShouldReturnApplicationsForAdmin() throws Exception {
        when(adminService.getAllApplications("Bearer token"))
                .thenReturn(List.of(buildApplication(1L, "user@finflow.com", "SUBMITTED")));

        mockMvc.perform(get("/admin/applications")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(username = "admin@finflow.com", roles = "ADMIN")
    void makeDecision_ShouldReturnDecisionResponse() throws Exception {
        DecisionRequest request = new DecisionRequest();
        request.setDecision(Decision.DecisionType.APPROVED);
        request.setRemarks("Approved");

        DecisionResponse response = DecisionResponse.builder()
                .id(1L)
                .applicationId(5L)
                .applicantEmail("user@finflow.com")
                .decision(Decision.DecisionType.APPROVED)
                .remarks("Approved")
                .decidedBy("admin@finflow.com")
                .decidedAt(LocalDateTime.now())
                .build();

        when(adminService.makeDecision(eq(5L), any(DecisionRequest.class), eq("admin@finflow.com"), eq("Bearer token")))
                .thenReturn(response);

        mockMvc.perform(post("/admin/applications/5/decision")
                        .header("Authorization", "Bearer token")
                        .principal(new UsernamePasswordAuthenticationToken("admin@finflow.com", null, List.of(() -> "ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("APPROVED"));
    }

    @Test
    @WithMockUser(username = "admin@finflow.com", roles = "ADMIN")
    void makeDecision_ShouldReturnBadRequest_WhenDecisionIsMissing() throws Exception {
        DecisionRequest request = new DecisionRequest();
        request.setRemarks("Missing decision");

        mockMvc.perform(post("/admin/applications/5/decision")
                        .header("Authorization", "Bearer token")
                        .principal(new UsernamePasswordAuthenticationToken("admin@finflow.com", null, List.of(() -> "ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(username = "admin@finflow.com", roles = "ADMIN")
    void getAllUsers_ShouldReturnUsers() throws Exception {
        UserResponse user = new UserResponse();
        user.setEmail("user@finflow.com");

        when(adminService.getAllUsers("Bearer token")).thenReturn(List.of(user));

        mockMvc.perform(get("/admin/users")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("user@finflow.com"));
    }

    @Test
    @WithMockUser(username = "admin@finflow.com", roles = "ADMIN")
    void getReport_ShouldReturnReport() throws Exception {
        ReportResponse report = ReportResponse.builder()
                .totalApplications(3)
                .approvalRate("33.3%")
                .build();

        when(adminService.generateReport("Bearer token")).thenReturn(report);

        mockMvc.perform(get("/admin/reports")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApplications").value(3))
                .andExpect(jsonPath("$.approvalRate").value("33.3%"));
    }

    @Test
    @WithMockUser(username = "admin@finflow.com", roles = "ADMIN")
    void getTodayReport_ShouldReturnTodayReport() throws Exception {
        when(adminService.getDailyReport(eq("Bearer token"), any(LocalDate.class)))
                .thenReturn(Map.of("date", LocalDate.now().toString(), "totalApplications", 2L));

        mockMvc.perform(get("/admin/reports/today")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApplications").value(2));
    }

    @Test
    @WithMockUser(username = "admin@finflow.com", roles = "ADMIN")
    void getDayReport_ShouldReturnSpecificDayReport() throws Exception {
        when(adminService.getDailyReport("Bearer token", LocalDate.of(2026, 3, 28)))
                .thenReturn(Map.of("date", "2026-03-28", "approved", 1L));

        mockMvc.perform(get("/admin/reports/day")
                        .header("Authorization", "Bearer token")
                        .param("date", "2026-03-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-03-28"));
    }

    @Test
    @WithMockUser(username = "user@finflow.com", roles = "APPLICANT")
    void getAllApplications_ShouldReturnForbidden_WhenApplicantAccessesAdminEndpoint() throws Exception {
        mockMvc.perform(get("/admin/applications")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    private ApplicationResponse buildApplication(Long id, String email, String status) {
        ApplicationResponse response = new ApplicationResponse();
        response.setId(id);
        response.setApplicantEmail(email);
        response.setStatus(status);
        response.setLoanAmount(new BigDecimal("500000"));
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }
}
