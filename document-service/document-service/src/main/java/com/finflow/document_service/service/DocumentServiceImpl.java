package com.finflow.document_service.service;

import com.finflow.document_service.config.RabbitConfig;
import com.finflow.document_service.dto.ApplicationStatusUpdateEvent;
import com.finflow.document_service.dto.CloudinaryResponse;
import com.finflow.document_service.dto.DocumentResponse;
import com.finflow.document_service.dto.VerifyRequest;
import com.finflow.document_service.entity.Document;
import com.finflow.document_service.entity.Document.DocumentStatus;
import com.finflow.document_service.entity.Document.DocumentType;
import com.finflow.document_service.exception.ApiException;
import com.finflow.document_service.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final CloudinaryService cloudinaryService;
    private final ApplicationStatusEventPublisher applicationStatusEventPublisher;

    @Override
    public DocumentResponse uploadDocument(MultipartFile file,
                                           Long applicationId,
                                           DocumentType documentType,
                                           String email,
                                           String authToken) throws IOException {
        CloudinaryResponse cloudinaryResponse =
                cloudinaryService.uploadFile(file, email + "/" + applicationId);
        String fileType = cloudinaryResponse.getFormat();

        if (fileType == null || fileType.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unable to determine uploaded file type");
        }

        Document document = Document.builder()
                .applicationId(applicationId)
                .applicantEmail(email)
                .fileName(cloudinaryResponse.getOriginalFileName())
                .storedFileName(cloudinaryResponse.getPublicId())
                .filePath(cloudinaryResponse.getSecureUrl())
                .cloudinaryPublicId(cloudinaryResponse.getPublicId())
                .cloudinaryUrl(cloudinaryResponse.getSecureUrl())
                .fileType(fileType.toUpperCase())
                .fileSize(cloudinaryResponse.getFileSize())
                .documentType(documentType)
                .build();

        DocumentResponse response = toResponse(documentRepository.save(document));
        updateApplicationStatus(applicationId, "DOCS_PENDING");
        return response;
    }

    @Override
    public List<DocumentResponse> getMyDocuments(String email) {
        return documentRepository.findByApplicantEmail(email)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentResponse> getDocumentsByApplication(Long applicationId,
                                                            String email) {
        return documentRepository
                .findByApplicationIdAndApplicantEmail(applicationId, email)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentResponse> getPendingDocuments() {
        return documentRepository
                .findByStatus(DocumentStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DocumentResponse verifyDocument(Long documentId,
                                           VerifyRequest request,
                                           String adminEmail,
                                           String authToken) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Document not found: " + documentId));

        document.setStatus(request.getStatus());
        document.setRemarks(request.getRemarks());
        document.setVerifiedBy(adminEmail);
        document.setVerifiedAt(LocalDateTime.now());

        DocumentResponse response = toResponse(documentRepository.save(document));

        Long applicationId = document.getApplicationId();
        List<Document> allDocs = documentRepository.findByApplicationId(applicationId);

        boolean allVerified = allDocs.stream()
                .allMatch(doc -> doc.getStatus() == DocumentStatus.VERIFIED);
        boolean anyRejected = allDocs.stream()
                .anyMatch(doc -> doc.getStatus() == DocumentStatus.REJECTED);

        if (anyRejected) {
            updateApplicationStatus(applicationId, "DOCS_PENDING");
            log.info("Document rejected. Application {} moved to DOCS_PENDING", applicationId);
        } else if (allVerified) {
            updateApplicationStatus(applicationId, "DOCS_VERIFIED");
            log.info("All documents verified. Application {} moved to DOCS_VERIFIED", applicationId);
        }

        return response;
    }

    private void updateApplicationStatus(Long applicationId, String status) {
        applicationStatusEventPublisher.publishStatusUpdate(applicationId, status);
    }

    private DocumentResponse toResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .applicationId(document.getApplicationId())
                .applicantEmail(document.getApplicantEmail())
                .fileName(document.getFileName())
                .cloudinaryUrl(document.getCloudinaryUrl())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .documentType(document.getDocumentType())
                .status(document.getStatus())
                .verifiedBy(document.getVerifiedBy())
                .remarks(document.getRemarks())
                .uploadedAt(document.getUploadedAt())
                .verifiedAt(document.getVerifiedAt())
                .build();
    }
}
