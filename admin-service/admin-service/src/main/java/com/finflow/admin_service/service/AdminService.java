package com.finflow.admin_service.service;

import com.finflow.admin_service.dto.ApplicationResponse;
import com.finflow.admin_service.dto.DecisionRequest;
import com.finflow.admin_service.dto.DecisionResponse;
import com.finflow.admin_service.dto.ReportResponse;
import com.finflow.admin_service.dto.UserResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AdminService {

    List<ApplicationResponse> getAllApplications(String authToken);

    DecisionResponse makeDecision(Long applicationId, DecisionRequest request, String adminEmail, String authToken);

    List<UserResponse> getAllUsers(String authToken);

    ReportResponse generateReport(String authToken);

    Map<String, Object> getDailyReport(String authToken, LocalDate date);
}
