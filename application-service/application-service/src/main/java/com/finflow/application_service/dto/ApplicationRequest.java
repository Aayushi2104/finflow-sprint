package com.finflow.application_service.dto;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class ApplicationRequest {

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
}
