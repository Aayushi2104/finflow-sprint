package com.finflow.application_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finflow.application_service.dto.ApplicationRequest;
import com.finflow.application_service.dto.ApplicationResponse;
import com.finflow.application_service.dto.StatusResponse;
import com.finflow.application_service.entity.LoanApplication.ApplicationStatus;
import com.finflow.application_service.exception.GlobalExceptionHandler;
import com.finflow.application_service.security.JwtAuthFilter;
import com.finflow.application_service.security.JwtUtil;
import com.finflow.application_service.service.ApplicationService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ApplicationControllerTest.TestSecurityConfig.class, GlobalExceptionHandler.class})
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApplicationService applicationService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    @WithMockUser(username = "user@finflow.com", roles = "APPLICANT")
    void create_ShouldReturnCreatedApplication() throws Exception {
        ApplicationRequest request = new ApplicationRequest();
        request.setFullName("Applicant");
        request.setLoanAmount(new BigDecimal("500000"));

        ApplicationResponse response = buildResponse(1L, "user@finflow.com", ApplicationStatus.DRAFT);

        when(applicationService.createApplication(any(ApplicationRequest.class), eq("user@finflow.com")))
                .thenReturn(response);

        mockMvc.perform(post("/applications")
                        .principal(new UsernamePasswordAuthenticationToken("user@finflow.com", null, List.of(() -> "ROLE_APPLICANT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @WithMockUser(username = "user@finflow.com", roles = "APPLICANT")
    void update_ShouldReturnUpdatedApplication() throws Exception {
        ApplicationRequest request = new ApplicationRequest();
        request.setAddress("New Address");

        ApplicationResponse response = buildResponse(2L, "user@finflow.com", ApplicationStatus.DRAFT);
        response.setAddress("New Address");

        when(applicationService.updateApplication(eq(2L), any(ApplicationRequest.class), eq("user@finflow.com")))
                .thenReturn(response);

        mockMvc.perform(put("/applications/2")
                        .principal(new UsernamePasswordAuthenticationToken("user@finflow.com", null, List.of(() -> "ROLE_APPLICANT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("New Address"));
    }

    @Test
    @WithMockUser(username = "user@finflow.com", roles = "APPLICANT")
    void submit_ShouldReturnSubmittedApplication() throws Exception {
        ApplicationResponse response = buildResponse(3L, "user@finflow.com", ApplicationStatus.SUBMITTED);

        when(applicationService.submitApplication(3L, "user@finflow.com")).thenReturn(response);

        mockMvc.perform(post("/applications/3/submit")
                        .principal(new UsernamePasswordAuthenticationToken("user@finflow.com", null, List.of(() -> "ROLE_APPLICANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @WithMockUser(username = "user@finflow.com", roles = "APPLICANT")
    void getMyApplications_ShouldReturnApplicantApplications() throws Exception {
        when(applicationService.getMyApplications("user@finflow.com"))
                .thenReturn(List.of(buildResponse(4L, "user@finflow.com", ApplicationStatus.DRAFT)));

        mockMvc.perform(get("/applications/my")
                        .principal(new UsernamePasswordAuthenticationToken("user@finflow.com", null, List.of(() -> "ROLE_APPLICANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(username = "user@finflow.com", roles = "APPLICANT")
    void getOne_ShouldReturnApplication() throws Exception {
        when(applicationService.getApplication(5L, "user@finflow.com"))
                .thenReturn(buildResponse(5L, "user@finflow.com", ApplicationStatus.DRAFT));

        mockMvc.perform(get("/applications/5")
                        .principal(new UsernamePasswordAuthenticationToken("user@finflow.com", null, List.of(() -> "ROLE_APPLICANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L));
    }

    @Test
    @WithMockUser(username = "user@finflow.com", roles = "APPLICANT")
    void getStatus_ShouldReturnStatusResponse() throws Exception {
        StatusResponse response = StatusResponse.builder()
                .applicationId(6L)
                .currentStatus(ApplicationStatus.DOCS_PENDING)
                .message("Please upload your KYC and income documents")
                .build();

        when(applicationService.getStatus(6L, "user@finflow.com")).thenReturn(response);

        mockMvc.perform(get("/applications/6/status")
                        .principal(new UsernamePasswordAuthenticationToken("user@finflow.com", null, List.of(() -> "ROLE_APPLICANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStatus").value("DOCS_PENDING"));
    }

    @Test
    @WithMockUser(username = "admin@finflow.com", roles = "ADMIN")
    void getAllApplications_ShouldReturnAllApplicationsForAdmin() throws Exception {
        when(applicationService.getAllApplications())
                .thenReturn(List.of(buildResponse(7L, "user@finflow.com", ApplicationStatus.SUBMITTED)));

        mockMvc.perform(get("/applications/admin/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(username = "admin@finflow.com", roles = "ADMIN")
    void updateStatus_ShouldReturnUpdatedStatus() throws Exception {
        when(applicationService.updateStatus(8L, ApplicationStatus.APPROVED))
                .thenReturn(buildResponse(8L, "user@finflow.com", ApplicationStatus.APPROVED));

        mockMvc.perform(put("/applications/admin/8/status")
                        .param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(username = "admin@finflow.com", roles = "ADMIN")
    void create_ShouldReturnForbidden_WhenAdminAccessesApplicantEndpoint() throws Exception {
        mockMvc.perform(post("/applications")
                        .principal(new UsernamePasswordAuthenticationToken("admin@finflow.com", null, List.of(() -> "ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
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

    private ApplicationResponse buildResponse(Long id, String email, ApplicationStatus status) {
        return ApplicationResponse.builder()
                .id(id)
                .applicantEmail(email)
                .fullName("Applicant")
                .loanAmount(new BigDecimal("500000"))
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
