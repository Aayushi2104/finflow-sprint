package com.finflow.document_service.dto;

import com.finflow.document_service.entity.Document.DocumentStatus;
import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class VerifyRequest {
    @NotNull
    private DocumentStatus status;
    private String remarks;
}
