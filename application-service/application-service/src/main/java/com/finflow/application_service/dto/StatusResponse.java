package com.finflow.application_service.dto;

import static com.finflow.application_service.entity.LoanApplication.ApplicationStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StatusResponse {
    private Long applicationId;
    private ApplicationStatus currentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
    private String message;
}
