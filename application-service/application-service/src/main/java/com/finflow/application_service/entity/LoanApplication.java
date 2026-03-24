package com.finflow.application_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_application")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoanApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
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

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private  ApplicationStatus status;


    @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime submittedAt;

    @PrePersist
    protected  void onCreate(){
        createdAt=LocalDateTime.now();
        updatedAt=LocalDateTime.now();
        status=ApplicationStatus.DRAFT;
    }

    @PostUpdate
    protected  void onUpdate(){
        updatedAt=LocalDateTime.now();
    }

    public enum ApplicationStatus{
        DRAFT,
        SUBMITTED,
        DOCS_PENDING,
        DOCS_VERIFIED,
        UNDER_REVIEW,
        APPROVED,
        REJECTED,
        CLOSED
    }
}
    
