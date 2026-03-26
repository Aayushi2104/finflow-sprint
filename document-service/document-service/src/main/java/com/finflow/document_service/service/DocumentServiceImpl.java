package com.finflow.document_service.service;

import com.finflow.document_service.dto.DocumentResponse;
import com.finflow.document_service.dto.VerifyRequest;
import com.finflow.document_service.entity.Document;
import com.finflow.document_service.entity.Document.DocumentType;
import com.finflow.document_service.exception.ApiException;
import com.finflow.document_service.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("pdf", "jpg", "jpeg", "png");

    private final DocumentRepository documentRepository;
    private final ModelMapper modelMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public DocumentResponse uploadDocument(MultipartFile file,
                                           Long applicationId,
                                           DocumentType documentType,
                                           String email) throws IOException {
        String extension = validateAndExtractExtension(file);
        String originalFileName = file.getOriginalFilename();
        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String storedFileName = UUID.randomUUID() + "." + extension;
        Path filePath = uploadPath.resolve(storedFileName);

        Files.copy(
                file.getInputStream(),
                filePath,
                StandardCopyOption.REPLACE_EXISTING
        );
        log.info("File saved: {}", filePath);

        Document document = Document.builder()
                .applicationId(applicationId)
                .applicantEmail(email)
                .fileName(originalFileName)
                .storedFileName(storedFileName)
                .filePath(filePath.toString())
                .fileType(extension.toUpperCase())
                .fileSize(file.getSize())
                .documentType(documentType)
                .build();

        return toResponse(documentRepository.save(document));
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
                .findByStatus(Document.DocumentStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
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

        return toResponse(documentRepository.save(document));
    }

    private String validateAndExtractExtension(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || !originalFileName.contains(".")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }

        String extension =
                originalFileName.substring(originalFileName.lastIndexOf('.') + 1)
                        .toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "File type not allowed. Only PDF, JPG, PNG allowed"
            );
        }

        return extension;
    }

    private DocumentResponse toResponse(Document document) {
        return modelMapper.map(document, DocumentResponse.class);
    }
}
