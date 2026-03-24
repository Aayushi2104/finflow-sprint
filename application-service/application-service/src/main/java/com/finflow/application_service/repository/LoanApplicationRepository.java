package com.finflow.application_service.repository;

import com.finflow.application_service.entity.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication,Long> {
    List<LoanApplication>findByApplicantEmailOrderByCreatedAtDesc(String email);
    List<LoanApplication>findAllByOrderByCreatedAtDesc();
}
