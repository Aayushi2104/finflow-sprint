package com.finflow.application_service.service;

import com.finflow.application_service.dto.ApplicationRequest;
import com.finflow.application_service.dto.ApplicationResponse;
import com.finflow.application_service.dto.StatusResponse;
import com.finflow.application_service.entity.LoanApplication;
import com.finflow.application_service.entity.LoanApplication.ApplicationStatus;
import com.finflow.application_service.exception.ApiException;
import com.finflow.application_service.repository.LoanApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock
    private LoanApplicationRepository repository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    @Test
    void createApplication_ShouldSaveApplicationForApplicant() {
        ApplicationRequest request = new ApplicationRequest();
        request.setFullName("Aayushi Jain");
        request.setLoanAmount(new BigDecimal("500000"));

        LoanApplication mapped = LoanApplication.builder()
                .fullName("Aayushi Jain")
                .loanAmount(new BigDecimal("500000"))
                .build();
        LoanApplication saved = buildApplication(1L, "aayushi@finflow.com", ApplicationStatus.DRAFT);
        ApplicationResponse response = buildResponse(1L, "aayushi@finflow.com", ApplicationStatus.DRAFT);

        when(modelMapper.map(request, LoanApplication.class)).thenReturn(mapped);
        when(repository.save(any(LoanApplication.class))).thenReturn(saved);
        doReturn(response).when(modelMapper).map(any(LoanApplication.class), eq(ApplicationResponse.class));

        ApplicationResponse actual = applicationService.createApplication(request, "aayushi@finflow.com");

        assertEquals(1L, actual.getId());
        assertEquals("aayushi@finflow.com", actual.getApplicantEmail());
        verify(repository).save(any(LoanApplication.class));
    }

    @Test
    void updateApplication_ShouldUpdateDraftApplication() {
        ApplicationRequest request = new ApplicationRequest();
        request.setAddress("New Address");

        LoanApplication existing = buildApplication(2L, "user@finflow.com", ApplicationStatus.DRAFT);
        LoanApplication saved = buildApplication(2L, "user@finflow.com", ApplicationStatus.DRAFT);
        saved.setAddress("New Address");
        ApplicationResponse response = buildResponse(2L, "user@finflow.com", ApplicationStatus.DRAFT);
        response.setAddress("New Address");

        when(repository.findById(2L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(saved);
        doAnswer(invocation -> {
            ApplicationRequest source = invocation.getArgument(0);
            LoanApplication destination = invocation.getArgument(1);
            destination.setAddress(source.getAddress());
            return destination;
        }).when(modelMapper).map(eq(request), eq(existing));
        doReturn(response).when(modelMapper).map(any(LoanApplication.class), eq(ApplicationResponse.class));

        ApplicationResponse actual = applicationService.updateApplication(2L, request, "user@finflow.com");

        assertEquals("New Address", actual.getAddress());
        verify(modelMapper).map(request, existing);
    }

    @Test
    void updateApplication_ShouldThrowBadRequest_WhenApplicationIsNotDraft() {
        LoanApplication existing = buildApplication(3L, "user@finflow.com", ApplicationStatus.SUBMITTED);
        when(repository.findById(3L)).thenReturn(Optional.of(existing));

        ApiException exception = assertThrows(ApiException.class,
                () -> applicationService.updateApplication(3L, new ApplicationRequest(), "user@finflow.com"));

        assertEquals("Only DRAFT applications can be updated", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void submitApplication_ShouldChangeStatusToSubmitted() {
        LoanApplication existing = buildApplication(4L, "user@finflow.com", ApplicationStatus.DRAFT);
        LoanApplication saved = buildApplication(4L, "user@finflow.com", ApplicationStatus.SUBMITTED);
        saved.setSubmittedAt(LocalDateTime.now());
        ApplicationResponse response = buildResponse(4L, "user@finflow.com", ApplicationStatus.SUBMITTED);
        response.setSubmittedAt(saved.getSubmittedAt());

        when(repository.findById(4L)).thenReturn(Optional.of(existing));
        when(repository.save(any(LoanApplication.class))).thenReturn(saved);
        doReturn(response).when(modelMapper).map(any(LoanApplication.class), eq(ApplicationResponse.class));

        ApplicationResponse actual = applicationService.submitApplication(4L, "user@finflow.com");

        assertEquals(ApplicationStatus.SUBMITTED, actual.getStatus());
        assertNotNull(actual.getSubmittedAt());
    }

    @Test
    void submitApplication_ShouldThrowBadRequest_WhenApplicationIsNotDraft() {
        LoanApplication existing = buildApplication(5L, "user@finflow.com", ApplicationStatus.APPROVED);
        when(repository.findById(5L)).thenReturn(Optional.of(existing));

        ApiException exception = assertThrows(ApiException.class,
                () -> applicationService.submitApplication(5L, "user@finflow.com"));

        assertEquals("Only DRAFT applications can be submitted", exception.getMessage());
    }

    @Test
    void getMyApplications_ShouldReturnApplicationsForApplicant() {
        LoanApplication application = buildApplication(6L, "user@finflow.com", ApplicationStatus.DRAFT);
        ApplicationResponse response = buildResponse(6L, "user@finflow.com", ApplicationStatus.DRAFT);

        when(repository.findByApplicantEmailOrderByCreatedAtDesc("user@finflow.com"))
                .thenReturn(List.of(application));
        doReturn(response).when(modelMapper).map(any(LoanApplication.class), eq(ApplicationResponse.class));

        List<ApplicationResponse> responses = applicationService.getMyApplications("user@finflow.com");

        assertEquals(1, responses.size());
        assertEquals(6L, responses.get(0).getId());
    }

    @Test
    void getApplication_ShouldThrowForbidden_WhenApplicationBelongsToAnotherApplicant() {
        LoanApplication application = buildApplication(7L, "other@finflow.com", ApplicationStatus.DRAFT);
        when(repository.findById(7L)).thenReturn(Optional.of(application));

        ApiException exception = assertThrows(ApiException.class,
                () -> applicationService.getApplication(7L, "user@finflow.com"));

        assertEquals("You don't have permission to access this application", exception.getMessage());
    }

    @Test
    void getStatus_ShouldReturnCurrentStatusMessage() {
        LoanApplication application = buildApplication(8L, "user@finflow.com", ApplicationStatus.DOCS_PENDING);
        when(repository.findById(8L)).thenReturn(Optional.of(application));

        StatusResponse response = applicationService.getStatus(8L, "user@finflow.com");

        assertEquals(8L, response.getApplicationId());
        assertEquals(ApplicationStatus.DOCS_PENDING, response.getCurrentStatus());
        assertEquals("Please upload your KYC and income documents", response.getMessage());
    }

    @Test
    void getAllApplications_ShouldReturnAllApplications() {
        LoanApplication application = buildApplication(9L, "user@finflow.com", ApplicationStatus.SUBMITTED);
        ApplicationResponse response = buildResponse(9L, "user@finflow.com", ApplicationStatus.SUBMITTED);

        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(application));
        doReturn(response).when(modelMapper).map(any(LoanApplication.class), eq(ApplicationResponse.class));

        List<ApplicationResponse> responses = applicationService.getAllApplications();

        assertEquals(1, responses.size());
        assertEquals(ApplicationStatus.SUBMITTED, responses.get(0).getStatus());
    }

    @Test
    void updateStatus_ShouldUpdateApplicationStatus() {
        LoanApplication application = buildApplication(10L, "user@finflow.com", ApplicationStatus.UNDER_REVIEW);
        LoanApplication saved = buildApplication(10L, "user@finflow.com", ApplicationStatus.APPROVED);
        ApplicationResponse response = buildResponse(10L, "user@finflow.com", ApplicationStatus.APPROVED);

        when(repository.findById(10L)).thenReturn(Optional.of(application));
        when(repository.save(application)).thenReturn(saved);
        doReturn(response).when(modelMapper).map(any(LoanApplication.class), eq(ApplicationResponse.class));

        ApplicationResponse actual = applicationService.updateStatus(10L, ApplicationStatus.APPROVED);

        assertEquals(ApplicationStatus.APPROVED, actual.getStatus());
    }

    private LoanApplication buildApplication(Long id, String email, ApplicationStatus status) {
        return LoanApplication.builder()
                .id(id)
                .applicantEmail(email)
                .fullName("Applicant")
                .loanAmount(new BigDecimal("500000"))
                .status(status)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private ApplicationResponse buildResponse(Long id, String email, ApplicationStatus status) {
        return ApplicationResponse.builder()
                .id(id)
                .applicantEmail(email)
                .fullName("Applicant")
                .loanAmount(new BigDecimal("500000"))
                .status(status)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
