package com.finflow.application_service.dto;

import com.finflow.application_service.entity.LoanApplication;
import static com.finflow.application_service.entity.LoanApplication.ApplicationStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationResponse {
    private Long id;
    private String applicantEmail;
    private String fullName;
    private String phone;
    private String address;
    private String dateOfBirth;
    private String employmentType;
    private String employerName;
    private BigDecimal monthlyIncome;
    private BigDecimal loanAmount;
    private Integer tenureMonths;
    private String loanPurpose;
    private BigDecimal interestRate;
    private BigDecimal emiAmount;
    private ApplicationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime submittedAt;

}
