package com.finflow.document_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Document {

@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
@Column(nullable = false)
    private Long applicationId;
@Column(nullable = false)
    private String applicantEmail;
@Column(nullable = false)
    private String fileName;

@Column(nullable = false)
    private String fileType;
    private Long fileSize;
    private String storedFileName;
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;


    private String cloudinaryPublicId;
    private String cloudinaryUrl;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;
    private String verifiedBy;
    private String remarks;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;
    private LocalDateTime verifiedAt;

    @PrePersist
    protected  void onCreate(){
        uploadedAt=LocalDateTime.now();
        status=DocumentStatus.PENDING;
    }
    public enum DocumentType{
        AADHAR,
        PAN_CARD,
        SALARY_SLIP,
        BANK_STATEMENT,
        ITR,
        OTHER
    }
    public enum DocumentStatus{
        PENDING,
        VERIFIED,
        REJECTED
    }

}
