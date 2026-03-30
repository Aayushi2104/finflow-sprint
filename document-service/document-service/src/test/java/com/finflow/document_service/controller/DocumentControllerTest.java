package com.finflow.document_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finflow.document_service.dto.DocumentResponse;
import com.finflow.document_service.dto.VerifyRequest;
import com.finflow.document_service.entity.Document.DocumentStatus;
import com.finflow.document_service.entity.Document.DocumentType;
import com.finflow.document_service.exception.GlobalExceptionHandler;
import com.finflow.document_service.security.JwtAuthFilter;
import com.finflow.document_service.security.JwtUtil;
import com.finflow.document_service.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({DocumentControllerTest.TestSecurityConfig.class, GlobalExceptionHandler.class})
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    @WithMockUser(username = "applicant@finflow.com", roles = "APPLICANT")
    void upload_ShouldReturnCreatedDocument_WhenApplicantIsAuthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "salary-slip.pdf",
                "application/pdf",
                "pdf-content".getBytes()
        );
        DocumentResponse response = buildResponse(
                1L,
                15L,
                "applicant@finflow.com",
                "salary-slip.pdf",
                DocumentType.SALARY_SLIP,
                DocumentStatus.PENDING
        );

        when(documentService.uploadDocument(
                any(),
                eq(15L),
                eq(DocumentType.SALARY_SLIP),
                eq("applicant@finflow.com"),
                eq("Bearer token")
        )).thenReturn(response);

        mockMvc.perform(multipart("/documents/upload")
                        .file(file)
                        .param("applicationId", "15")
                        .param("documentType", "SALARY_SLIP")
                        .header("Authorization", "Bearer token")
                        .principal(new UsernamePasswordAuthenticationToken(
                                "applicant@finflow.com",
                                null,
                                List.of(() -> "ROLE_APPLICANT")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.fileName").value("salary-slip.pdf"))
                .andExpect(jsonPath("$.documentType").value("SALARY_SLIP"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(documentService).uploadDocument(
                any(),
                eq(15L),
                eq(DocumentType.SALARY_SLIP),
                eq("applicant@finflow.com"),
                eq("Bearer token")
        );
    }

    @Test
    @WithMockUser(username = "admin@finflow.com", roles = "ADMIN")
    void upload_ShouldReturnForbidden_WhenAdminTriesApplicantEndpoint() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "salary-slip.pdf",
                "application/pdf",
                "pdf-content".getBytes()
        );

        mockMvc.perform(multipart("/documents/upload")
                        .file(file)
                        .param("applicationId", "15")
                        .param("documentType", "SALARY_SLIP")
                        .header("Authorization", "Bearer token")
                        .principal(new UsernamePasswordAuthenticationToken(
                                "admin@finflow.com",
                                null,
                                List.of(() -> "ROLE_ADMIN")
                        )))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "applicant@finflow.com", roles = "APPLICANT")
    void getMyDocuments_ShouldReturnApplicantDocuments() throws Exception {
        List<DocumentResponse> responses = List.of(
                buildResponse(1L, 15L, "applicant@finflow.com", "aadhar.pdf", DocumentType.AADHAR, DocumentStatus.PENDING),
                buildResponse(2L, 15L, "applicant@finflow.com", "pan.png", DocumentType.PAN_CARD, DocumentStatus.VERIFIED)
        );

        when(documentService.getMyDocuments("applicant@finflow.com")).thenReturn(responses);

        mockMvc.perform(get("/documents/my")
                        .principal(new UsernamePasswordAuthenticationToken(
                                "applicant@finflow.com",
                                null,
                                List.of(() -> "ROLE_APPLICANT")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fileName").value("aadhar.pdf"))
                .andExpect(jsonPath("$[1].status").value("VERIFIED"));
    }

    @Test
    @WithMockUser(username = "applicant@finflow.com", roles = "APPLICANT")
    void getDocumentsByApplication_ShouldReturnDocumentsForApplication() throws Exception {
        List<DocumentResponse> responses = List.of(
                buildResponse(3L, 99L, "applicant@finflow.com", "itr.pdf", DocumentType.ITR, DocumentStatus.PENDING)
        );

        when(documentService.getDocumentsByApplication(99L, "applicant@finflow.com"))
                .thenReturn(responses);

        mockMvc.perform(get("/documents/application/99")
                        .principal(new UsernamePasswordAuthenticationToken(
                                "applicant@finflow.com",
                                null,
                                List.of(() -> "ROLE_APPLICANT")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].applicationId").value(99))
                .andExpect(jsonPath("$[0].documentType").value("ITR"));
    }

    @Test
    @WithMockUser(username = "applicant@finflow.com", roles = "APPLICANT")
    void getPending_ShouldReturnForbidden_WhenApplicantTriesAdminEndpoint() throws Exception {
        mockMvc.perform(get("/documents/admin/pending")
                        .principal(new UsernamePasswordAuthenticationToken(
                                "applicant@finflow.com",
                                null,
                                List.of(() -> "ROLE_APPLICANT")
                        )))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@finflow.com", roles = "ADMIN")
    void getPending_ShouldReturnPendingDocuments_WhenAdminIsAuthorized() throws Exception {
        List<DocumentResponse> responses = List.of(
                buildResponse(5L, 77L, "applicant1@finflow.com", "pending-aadhar.pdf", DocumentType.AADHAR, DocumentStatus.PENDING),
                buildResponse(6L, 78L, "applicant2@finflow.com", "pending-pan.pdf", DocumentType.PAN_CARD, DocumentStatus.PENDING)
        );

        when(documentService.getPendingDocuments()).thenReturn(responses);

        mockMvc.perform(get("/documents/admin/pending")
                        .principal(new UsernamePasswordAuthenticationToken(
                                "admin@finflow.com",
                                null,
                                List.of(() -> "ROLE_ADMIN")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[1].fileName").value("pending-pan.pdf"));
    }

    @Test
    @WithMockUser(username = "admin@finflow.com", roles = "ADMIN")
    void verify_ShouldReturnUpdatedDocument_WhenAdminIsAuthorized() throws Exception {
        VerifyRequest request = new VerifyRequest();
        request.setStatus(DocumentStatus.VERIFIED);
        request.setRemarks("Approved");

        DocumentResponse response = buildResponse(
                4L,
                44L,
                "applicant@finflow.com",
                "bank.pdf",
                DocumentType.BANK_STATEMENT,
                DocumentStatus.VERIFIED
        );
        response.setVerifiedBy("admin@finflow.com");
        response.setVerifiedAt(LocalDateTime.now());
        response.setRemarks("Approved");

        when(documentService.verifyDocument(
                eq(4L),
                any(VerifyRequest.class),
                eq("admin@finflow.com"),
                eq("Bearer admin-token")
        )).thenReturn(response);

        mockMvc.perform(put("/documents/admin/4/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer admin-token")
                        .principal(new UsernamePasswordAuthenticationToken(
                                "admin@finflow.com",
                                null,
                                List.of(() -> "ROLE_ADMIN")
                        ))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4L))
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.verifiedBy").value("admin@finflow.com"))
                .andExpect(jsonPath("$.remarks").value("Approved"));
    }

    @Test
    @WithMockUser(username = "admin@finflow.com", roles = "ADMIN")
    void verify_ShouldReturnBadRequest_WhenStatusIsMissing() throws Exception {
        VerifyRequest request = new VerifyRequest();
        request.setRemarks("Missing status");

        mockMvc.perform(put("/documents/admin/4/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer admin-token")
                        .principal(new UsernamePasswordAuthenticationToken(
                                "admin@finflow.com",
                                null,
                                List.of(() -> "ROLE_ADMIN")
                        ))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.status").exists());
    }

    @Test
    void getMyDocuments_ShouldReturnUnauthorized_WhenUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(get("/documents/my"))
                .andExpect(status().isNotFound());
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .httpBasic(basic -> {
                    })
                    .formLogin(form -> form.disable());
            return http.build();
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    private DocumentResponse buildResponse(Long id,
                                           Long applicationId,
                                           String email,
                                           String fileName,
                                           DocumentType documentType,
                                           DocumentStatus status) {
        return DocumentResponse.builder()
                .id(id)
                .applicationId(applicationId)
                .applicantEmail(email)
                .fileName(fileName)
                .cloudinaryUrl("https://res.cloudinary.com/demo/image/upload/v1/" + fileName)
                .fileType("PDF")
                .fileSize(2048L)
                .documentType(documentType)
                .status(status)
                .uploadedAt(LocalDateTime.now())
                .build();
    }
}
