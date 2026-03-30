package com.finflow.admin_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public class ReportResponse {
    private long totalApplications;
    private long totalDraft;
    private long totalSubmitted;
    private long totalDocsPending;
    private long totalDocsVerified;
    private long totalUnderReview;
    private long totalApproved;
    private long totalRejected;
    private long totalClosed;
    private BigDecimal totalLoanAmountRequested;
    private BigDecimal totalLoanAmountApproved;
    private String approvalRate;
    private String rejectionRate;
    private String pendingRate;
    private Map<String, Long> applicationsByMonth;
    private Map<String, Long> applicationsByStatus;
    private Map<String, Long> applicationsByDay;
    private Map<String, Long> approvalsByDay;
    private Map<String, Long> rejectionsByDay;
    private Map<String, BigDecimal> loanAmountByDay;
    private LocalDateTime generatedAt;

    public static Builder builder() {
        return new Builder();
    }

    public long getTotalApplications() { return totalApplications; }
    public long getTotalDraft() { return totalDraft; }
    public long getTotalSubmitted() { return totalSubmitted; }
    public long getTotalDocsPending() { return totalDocsPending; }
    public long getTotalDocsVerified() { return totalDocsVerified; }
    public long getTotalUnderReview() { return totalUnderReview; }
    public long getTotalApproved() { return totalApproved; }
    public long getTotalRejected() { return totalRejected; }
    public long getTotalClosed() { return totalClosed; }
    public BigDecimal getTotalLoanAmountRequested() { return totalLoanAmountRequested; }
    public BigDecimal getTotalLoanAmountApproved() { return totalLoanAmountApproved; }
    public String getApprovalRate() { return approvalRate; }
    public String getRejectionRate() { return rejectionRate; }
    public String getPendingRate() { return pendingRate; }
    public Map<String, Long> getApplicationsByMonth() { return applicationsByMonth; }
    public Map<String, Long> getApplicationsByStatus() { return applicationsByStatus; }
    public Map<String, Long> getApplicationsByDay() { return applicationsByDay; }
    public Map<String, Long> getApprovalsByDay() { return approvalsByDay; }
    public Map<String, Long> getRejectionsByDay() { return rejectionsByDay; }
    public Map<String, BigDecimal> getLoanAmountByDay() { return loanAmountByDay; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }

    public static class Builder {
        private final ReportResponse target = new ReportResponse();

        public Builder totalApplications(long value) { target.totalApplications = value; return this; }
        public Builder totalDraft(long value) { target.totalDraft = value; return this; }
        public Builder totalSubmitted(long value) { target.totalSubmitted = value; return this; }
        public Builder totalDocsPending(long value) { target.totalDocsPending = value; return this; }
        public Builder totalDocsVerified(long value) { target.totalDocsVerified = value; return this; }
        public Builder totalUnderReview(long value) { target.totalUnderReview = value; return this; }
        public Builder totalApproved(long value) { target.totalApproved = value; return this; }
        public Builder totalRejected(long value) { target.totalRejected = value; return this; }
        public Builder totalClosed(long value) { target.totalClosed = value; return this; }
        public Builder totalLoanAmountRequested(BigDecimal value) { target.totalLoanAmountRequested = value; return this; }
        public Builder totalLoanAmountApproved(BigDecimal value) { target.totalLoanAmountApproved = value; return this; }
        public Builder approvalRate(String value) { target.approvalRate = value; return this; }
        public Builder rejectionRate(String value) { target.rejectionRate = value; return this; }
        public Builder pendingRate(String value) { target.pendingRate = value; return this; }
        public Builder applicationsByMonth(Map<String, Long> value) { target.applicationsByMonth = value; return this; }
        public Builder applicationsByStatus(Map<String, Long> value) { target.applicationsByStatus = value; return this; }
        public Builder applicationsByDay(Map<String, Long> value) { target.applicationsByDay = value; return this; }
        public Builder approvalsByDay(Map<String, Long> value) { target.approvalsByDay = value; return this; }
        public Builder rejectionsByDay(Map<String, Long> value) { target.rejectionsByDay = value; return this; }
        public Builder loanAmountByDay(Map<String, BigDecimal> value) { target.loanAmountByDay = value; return this; }
        public Builder generatedAt(LocalDateTime value) { target.generatedAt = value; return this; }
        public ReportResponse build() { return target; }
    }
}
