package com.finflow.document_service.service;

import com.finflow.document_service.dto.DocumentResponse;
import com.finflow.document_service.dto.VerifyRequest;
import com.finflow.document_service.entity.Document.DocumentType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface DocumentService {

    DocumentResponse uploadDocument(MultipartFile file,
                                    Long applicationId,
                                    DocumentType documentType,
                                    String email,
                                    String authToken) throws IOException;

    List<DocumentResponse> getMyDocuments(String email);

    List<DocumentResponse> getDocumentsByApplication(Long applicationId, String email);

    List<DocumentResponse> getPendingDocuments();

    DocumentResponse verifyDocument(Long documentId,
                                    VerifyRequest request,
                                    String adminEmail,
                                    String authToken);
}
