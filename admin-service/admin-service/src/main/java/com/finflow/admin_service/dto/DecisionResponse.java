package com.finflow.admin_service.dto;

import com.finflow.admin_service.entity.Decision.DecisionType;

import java.time.LocalDateTime;

public class DecisionResponse {
    private Long id;
    private Long applicationId;
    private String applicantEmail;
    private DecisionType decision;
    private String remarks;
    private String decidedBy;
    private LocalDateTime decidedAt;

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() { return id; }
    public Long getApplicationId() { return applicationId; }
    public String getApplicantEmail() { return applicantEmail; }
    public DecisionType getDecision() { return decision; }
    public String getRemarks() { return remarks; }
    public String getDecidedBy() { return decidedBy; }
    public LocalDateTime getDecidedAt() { return decidedAt; }

    public static class Builder {
        private final DecisionResponse target = new DecisionResponse();

        public Builder id(Long id) { target.id = id; return this; }
        public Builder applicationId(Long applicationId) { target.applicationId = applicationId; return this; }
        public Builder applicantEmail(String applicantEmail) { target.applicantEmail = applicantEmail; return this; }
        public Builder decision(DecisionType decision) { target.decision = decision; return this; }
        public Builder remarks(String remarks) { target.remarks = remarks; return this; }
        public Builder decidedBy(String decidedBy) { target.decidedBy = decidedBy; return this; }
        public Builder decidedAt(LocalDateTime decidedAt) { target.decidedAt = decidedAt; return this; }
        public DecisionResponse build() { return target; }
    }
}
