package com.finflow.document_service.dto;

import com.finflow.document_service.entity.Document.DocumentStatus;
import com.finflow.document_service.entity.Document.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    private Long id;
    private Long applicationId;
    private String applicantEmail;
    private String fileName;
    private String cloudinaryUrl;
    private String fileType;
    private Long fileSize;
    private DocumentType documentType;
    private DocumentStatus status;
    private String verifiedBy;
    private String remarks;
    private LocalDateTime uploadedAt;
    private LocalDateTime verifiedAt;
}
