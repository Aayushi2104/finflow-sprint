package com.finflow.document_service.service;

import com.finflow.document_service.dto.DocumentResponse;
import com.finflow.document_service.dto.VerifyRequest;
import com.finflow.document_service.entity.Document;
import com.finflow.document_service.entity.Document.DocumentType;
import com.finflow.document_service.exception.ApiException;
import com.finflow.document_service.mapper.DocumentMapper;
import com.finflow.document_service.repository.DocumentRepository;
import com.finflow.document_service.storage.DocumentStorageService;
import com.finflow.document_service.storage.StoredDocumentFile;
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
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentStorageService documentStorageService;
    private final DocumentMapper documentMapper;


    public DocumentResponse uploadDocument(MultipartFile file,
                                           Long applicationId,
                                           DocumentType documentType,
                                           String email) throws IOException {
        StoredDocumentFile storedFile = documentStorageService.store(file);
        log.info("File saved: {}", storedFile.filePath());

        Document document = documentMapper.toNewDocument(
                applicationId,
                documentType,
                email,
                storedFile
        );

        return documentMapper.toResponse(documentRepository.save(document));
    }


    public List<DocumentResponse> getMyDocuments(String email) {
        return documentRepository.findByApplicantEmail(email)
                .stream()
                .map(documentMapper::toResponse)
                .collect(Collectors.toList());
    }


    public List<DocumentResponse> getDocumentsByApplication(Long applicationId,
                                                            String email) {
        return documentRepository
                .findByApplicationIdAndApplicantEmail(applicationId, email)
                .stream()
                .map(documentMapper::toResponse)
                .collect(Collectors.toList());
    }


    public List<DocumentResponse> getPendingDocuments() {
        return documentRepository
                .findByStatus(Document.DocumentStatus.PENDING)
                .stream()
                .map(documentMapper::toResponse)
                .collect(Collectors.toList());
    }


    public DocumentResponse verifyDocument(Long documentId,
                                           VerifyRequest request,
                                           String adminEmail) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Document not found: " + documentId));

        document.setStatus(request.getStatus());
        document.setRemarks(request.getRemarks());
        document.setVerifiedBy(adminEmail);
        document.setVerifiedAt(LocalDateTime.now());

        return documentMapper.toResponse(documentRepository.save(document));
    }
}
