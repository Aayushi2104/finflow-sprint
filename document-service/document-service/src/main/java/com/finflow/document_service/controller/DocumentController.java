package com.finflow.document_service.controller;

import com.finflow.document_service.dto.DocumentResponse;
import com.finflow.document_service.dto.VerifyRequest;
import com.finflow.document_service.entity.Document.DocumentType;
import com.finflow.document_service.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping({"/documents"})
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;


    @PostMapping("/upload")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<DocumentResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("applicationId") Long applicationId,
            @RequestParam("documentType") DocumentType documentType,
            Authentication auth) throws IOException {

        return ResponseEntity.ok(
                documentService.uploadDocument(
                        file, applicationId, documentType, auth.getName())
        );
    }


    @GetMapping("/my")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<List<DocumentResponse>> getMyDocuments(
            Authentication auth) {
        return ResponseEntity.ok(
                documentService.getMyDocuments(auth.getName())
        );
    }


    @GetMapping("/application/{applicationId}")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<List<DocumentResponse>> getByApplication(
            @PathVariable Long applicationId,
            Authentication auth) {
        return ResponseEntity.ok(
                documentService.getDocumentsByApplication(
                        applicationId, auth.getName())
        );
    }


    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DocumentResponse>> getPending() {
        return ResponseEntity.ok(
                documentService.getPendingDocuments()
        );
    }


    @PutMapping("/admin/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentResponse> verify(
            @PathVariable Long id,
            @Valid @RequestBody VerifyRequest request,
            Authentication auth) {
        return ResponseEntity.ok(
                documentService.verifyDocument(id, request, auth.getName())
        );
    }
}
