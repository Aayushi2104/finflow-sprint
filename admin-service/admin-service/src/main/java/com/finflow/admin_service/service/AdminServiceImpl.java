package com.finflow.admin_service.service;

import com.finflow.admin_service.dto.ApplicationResponse;
import com.finflow.admin_service.dto.DecisionRequest;
import com.finflow.admin_service.dto.DecisionResponse;
import com.finflow.admin_service.dto.ReportResponse;
import com.finflow.admin_service.dto.UserResponse;
import com.finflow.admin_service.entity.Decision;
import com.finflow.admin_service.exception.ApiException;
import com.finflow.admin_service.repository.DecisionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final DecisionRepository decisionRepository;
    private final AdminDownstreamService adminDownstreamService;

    public AdminServiceImpl(DecisionRepository decisionRepository,
                            AdminDownstreamService adminDownstreamService) {
        this.decisionRepository = decisionRepository;
        this.adminDownstreamService = adminDownstreamService;
    }

    @Override
    @Cacheable(cacheNames = "admin.applications", key = "'all'")
    public List<ApplicationResponse> getAllApplications(String authToken) {
        log.info("CACHE MISS - loading all applications from downstream application-service");
        return adminDownstreamService.getAllApplications(authToken);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "admin.applications", allEntries = true),
            @CacheEvict(cacheNames = "admin.reports", allEntries = true)
    })
    public DecisionResponse makeDecision(Long applicationId, DecisionRequest request, String adminEmail, String authToken) {
        if (decisionRepository.findByApplicationId(applicationId).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Decision already made for application: " + applicationId);
        }

        updateApplicationStatus(applicationId, "UNDER_REVIEW", authToken);

        String newStatus = request.getDecision() == Decision.DecisionType.APPROVED ? "APPROVED" : "REJECTED";
        updateApplicationStatus(applicationId, newStatus, authToken);

        Decision decision = Decision.builder()
                .applicationId(applicationId)
                .applicantEmail(getApplicantEmail(applicationId, authToken))
                .decision(request.getDecision())
                .remarks(request.getRemarks())
                .decidedBy(adminEmail)
                .build();

        Decision saved = decisionRepository.save(decision);
        return toDecisionResponse(saved);
    }

    @Override
    @Cacheable(cacheNames = "admin.users", key = "'all'")
    public List<UserResponse> getAllUsers(String authToken) {
        log.info("CACHE MISS - loading all users from downstream auth-service");
        return adminDownstreamService.getAllUsers(authToken);
    }

    @Override
    @Cacheable(cacheNames = "admin.reports", key = "'summary'")
    public ReportResponse generateReport(String authToken) {
        log.info("CACHE MISS - generating summary report");
        List<ApplicationResponse> applications = getAllApplications(authToken);

        if (applications == null || applications.isEmpty()) {
            return buildEmptyReport();
        }

        long total = applications.size();

        Map<String, Long> byStatus = applications.stream()
                .collect(Collectors.groupingBy(ApplicationResponse::getStatus, Collectors.counting()));

        long totalApproved = byStatus.getOrDefault("APPROVED", 0L);
        long totalRejected = byStatus.getOrDefault("REJECTED", 0L);
        long totalDraft = byStatus.getOrDefault("DRAFT", 0L);
        long totalSubmitted = byStatus.getOrDefault("SUBMITTED", 0L);
        long totalDocsPending = byStatus.getOrDefault("DOCS_PENDING", 0L);
        long totalDocsVerified = byStatus.getOrDefault("DOCS_VERIFIED", 0L);
        long totalUnderReview = byStatus.getOrDefault("UNDER_REVIEW", 0L);
        long totalClosed = byStatus.getOrDefault("CLOSED", 0L);

        BigDecimal totalRequested = applications.stream()
                .map(a -> a.getLoanAmount() != null ? a.getLoanAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalApprovedAmount = applications.stream()
                .filter(a -> "APPROVED".equals(a.getStatus()))
                .map(a -> a.getLoanAmount() != null ? a.getLoanAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> byMonth = applications.stream()
                .filter(a -> a.getCreatedAt() != null)
                .collect(Collectors.groupingBy(a -> a.getCreatedAt().getMonth().name(), Collectors.counting()));

        Map<String, Long> byDay = applications.stream()
                .filter(a -> a.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getCreatedAt().toLocalDate().toString(),
                        TreeMap::new,
                        Collectors.counting()
                ));

        Map<String, Long> approvalsByDay = applications.stream()
                .filter(a -> "APPROVED".equals(a.getStatus()) && a.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getCreatedAt().toLocalDate().toString(),
                        TreeMap::new,
                        Collectors.counting()
                ));

        Map<String, Long> rejectionsByDay = applications.stream()
                .filter(a -> "REJECTED".equals(a.getStatus()) && a.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getCreatedAt().toLocalDate().toString(),
                        TreeMap::new,
                        Collectors.counting()
                ));

        Map<String, BigDecimal> loanAmountByDay = applications.stream()
                .filter(a -> a.getCreatedAt() != null && a.getLoanAmount() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getCreatedAt().toLocalDate().toString(),
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, ApplicationResponse::getLoanAmount, BigDecimal::add)
                ));

        return ReportResponse.builder()
                .totalApplications(total)
                .totalDraft(totalDraft)
                .totalSubmitted(totalSubmitted)
                .totalDocsPending(totalDocsPending)
                .totalDocsVerified(totalDocsVerified)
                .totalUnderReview(totalUnderReview)
                .totalApproved(totalApproved)
                .totalRejected(totalRejected)
                .totalClosed(totalClosed)
                .totalLoanAmountRequested(totalRequested)
                .totalLoanAmountApproved(totalApprovedAmount)
                .approvalRate(calculateRate(totalApproved, total))
                .rejectionRate(calculateRate(totalRejected, total))
                .pendingRate(calculateRate(totalSubmitted + totalDocsPending + totalUnderReview, total))
                .applicationsByStatus(byStatus)
                .applicationsByMonth(byMonth)
                .applicationsByDay(byDay)
                .approvalsByDay(approvalsByDay)
                .rejectionsByDay(rejectionsByDay)
                .loanAmountByDay(loanAmountByDay)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Cacheable(cacheNames = "admin.reports", key = "'daily:' + #date")
    public Map<String, Object> getDailyReport(String authToken, LocalDate date) {
        log.info("CACHE MISS - generating daily report for {}", date);
        List<ApplicationResponse> applications = getAllApplications(authToken);

        List<ApplicationResponse> dayApps = applications.stream()
                .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().toLocalDate().equals(date))
                .toList();

        long total = dayApps.size();
        long approved = dayApps.stream().filter(a -> "APPROVED".equals(a.getStatus())).count();
        long rejected = dayApps.stream().filter(a -> "REJECTED".equals(a.getStatus())).count();
        long submitted = dayApps.stream().filter(a -> "SUBMITTED".equals(a.getStatus())).count();
        long underReview = dayApps.stream().filter(a -> "UNDER_REVIEW".equals(a.getStatus())).count();

        BigDecimal totalAmount = dayApps.stream()
                .map(a -> a.getLoanAmount() != null ? a.getLoanAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal approvedAmount = dayApps.stream()
                .filter(a -> "APPROVED".equals(a.getStatus()))
                .map(a -> a.getLoanAmount() != null ? a.getLoanAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("date", date.toString());
        report.put("totalApplications", total);
        report.put("submitted", submitted);
        report.put("underReview", underReview);
        report.put("approved", approved);
        report.put("rejected", rejected);
        report.put("totalLoanRequested", totalAmount);
        report.put("totalLoanApproved", approvedAmount);
        report.put("approvalRate", calculateRate(approved, total));
        report.put("rejectionRate", calculateRate(rejected, total));
        report.put("generatedAt", LocalDateTime.now());
        return report;
    }

    private void updateApplicationStatus(Long applicationId, String status, String authToken) {
        adminDownstreamService.updateApplicationStatus(applicationId, status, authToken);
    }

    private String getApplicantEmail(Long applicationId, String authToken) {
        try {
            return Objects.requireNonNull(adminDownstreamService.getAllApplications(authToken))
                    .stream()
                    .filter(a -> a.getId().equals(applicationId))
                    .findFirst()
                    .map(ApplicationResponse::getApplicantEmail)
                    .orElse("unknown");
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String calculateRate(long count, long total) {
        if (total == 0) {
            return "0.0%";
        }
        double rate = (double) count / total * 100;
        return String.format("%.1f%%", rate);
    }

    private DecisionResponse toDecisionResponse(Decision decision) {
        return DecisionResponse.builder()
                .id(decision.getId())
                .applicationId(decision.getApplicationId())
                .applicantEmail(decision.getApplicantEmail())
                .decision(decision.getDecision())
                .remarks(decision.getRemarks())
                .decidedBy(decision.getDecidedBy())
                .decidedAt(decision.getDecidedAt())
                .build();
    }

    private ReportResponse buildEmptyReport() {
        return ReportResponse.builder()
                .totalApplications(0)
                .totalDraft(0)
                .totalSubmitted(0)
                .totalDocsPending(0)
                .totalDocsVerified(0)
                .totalUnderReview(0)
                .totalApproved(0)
                .totalRejected(0)
                .totalClosed(0)
                .totalLoanAmountRequested(BigDecimal.ZERO)
                .totalLoanAmountApproved(BigDecimal.ZERO)
                .approvalRate("0.0%")
                .rejectionRate("0.0%")
                .pendingRate("0.0%")
                .applicationsByMonth(Map.of())
                .applicationsByStatus(Map.of())
                .applicationsByDay(Map.of())
                .approvalsByDay(Map.of())
                .rejectionsByDay(Map.of())
                .loanAmountByDay(Map.of())
                .generatedAt(LocalDateTime.now())
                .build();
    }

}
