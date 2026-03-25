package com.finflow.document_service.mapper;

import com.finflow.document_service.dto.DocumentResponse;
import com.finflow.document_service.entity.Document;
import com.finflow.document_service.entity.Document.DocumentType;
import com.finflow.document_service.storage.StoredDocumentFile;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public Document toNewDocument(
            Long applicationId,
            DocumentType documentType,
            String applicantEmail,
            StoredDocumentFile storedFile
    ) {
        return Document.builder()
                .applicationId(applicationId)
                .applicantEmail(applicantEmail)
                .fileName(storedFile.originalFileName())
                .storedFileName(storedFile.storedFileName())
                .filePath(storedFile.filePath())
                .fileType(storedFile.fileExtension().toUpperCase())
                .fileSize(storedFile.fileSize())
                .documentType(documentType)
                .build();
    }

    public DocumentResponse toResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .applicationId(document.getApplicationId())
                .applicantEmail(document.getApplicantEmail())
                .fileName(document.getFileName())
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
