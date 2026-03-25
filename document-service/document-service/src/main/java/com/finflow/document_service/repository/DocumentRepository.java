package com.finflow.document_service.repository;

import com.finflow.document_service.entity.Document;
import com.finflow.document_service.entity.Document.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface  DocumentRepository extends
        JpaRepository<Document, Long> {
    List<Document> findByApplicantEmail(String email);

    List<Document> findByApplicationId(Long applicationId);

    List<Document> findByStatus(DocumentStatus status);

    List<Document> findByApplicationIdAndApplicantEmail(Long applicationId, String email);

}
