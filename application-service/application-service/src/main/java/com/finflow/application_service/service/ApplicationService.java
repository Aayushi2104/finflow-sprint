package com.finflow.application_service.service;

import com.finflow.application_service.dto.ApplicationRequest;
import com.finflow.application_service.dto.ApplicationResponse;
import com.finflow.application_service.dto.StatusResponse;
import com.finflow.application_service.entity.LoanApplication.ApplicationStatus;

import java.util.List;

public interface ApplicationService {

    ApplicationResponse createApplication(ApplicationRequest request, String email);

    ApplicationResponse updateApplication(Long id, ApplicationRequest request, String email);

    ApplicationResponse submitApplication(Long id, String email);

    List<ApplicationResponse> getMyApplications(String email);

    ApplicationResponse getApplication(Long id, String email);

    StatusResponse getStatus(Long id, String email);

    List<ApplicationResponse> getAllApplications();

    ApplicationResponse updateStatus(Long id, ApplicationStatus newStatus);
}
