package com.finflow.admin_service.dto;

import com.finflow.admin_service.entity.Decision.DecisionType;
import jakarta.validation.constraints.NotNull;

public class DecisionRequest {

    @NotNull
    private DecisionType decision;

    private String remarks;

    public DecisionType getDecision() {
        return decision;
    }

    public void setDecision(DecisionType decision) {
        this.decision = decision;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
