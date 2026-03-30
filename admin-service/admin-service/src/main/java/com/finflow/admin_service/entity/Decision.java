package com.finflow.admin_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "decisions")
public class Decision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long applicationId;

    @Column(nullable = false)
    private String applicantEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DecisionType decision;

    private String remarks;

    @Column(nullable = false)
    private String decidedBy;

    private LocalDateTime decidedAt;

    @PrePersist
    protected void onCreate() {
        decidedAt = LocalDateTime.now();
    }

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
        private final Decision target = new Decision();

        public Builder applicationId(Long applicationId) { target.applicationId = applicationId; return this; }
        public Builder applicantEmail(String applicantEmail) { target.applicantEmail = applicantEmail; return this; }
        public Builder decision(DecisionType decision) { target.decision = decision; return this; }
        public Builder remarks(String remarks) { target.remarks = remarks; return this; }
        public Builder decidedBy(String decidedBy) { target.decidedBy = decidedBy; return this; }
        public Decision build() { return target; }
    }

    public enum DecisionType {
        APPROVED, REJECTED
    }
}
