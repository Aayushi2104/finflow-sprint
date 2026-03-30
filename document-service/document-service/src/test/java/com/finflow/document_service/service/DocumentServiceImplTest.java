package com.finflow.document_service.service;

import com.finflow.document_service.dto.CloudinaryResponse;
import com.finflow.document_service.dto.DocumentResponse;
import com.finflow.document_service.dto.VerifyRequest;
import com.finflow.document_service.entity.Document;
import com.finflow.document_service.entity.Document.DocumentStatus;
import com.finflow.document_service.entity.Document.DocumentType;
import com.finflow.document_service.exception.ApiException;
import com.finflow.document_service.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private ApplicationStatusEventPublisher applicationStatusEventPublisher;

    @InjectMocks
    private DocumentServiceImpl documentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(documentService, "applicationStatusEventPublisher", applicationStatusEventPublisher);
    }

    @Test
    void uploadDocument_ShouldUploadToCloudinary_SaveDocument_AndUpdateApplicationStatus() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "salary-slip.pdf",
                "application/pdf",
                "test-pdf".getBytes()
        );
        CloudinaryResponse cloudinaryResponse = CloudinaryResponse.builder()
                .publicId("finflow/test/public-id")
                .secureUrl("https://res.cloudinary.com/demo/image/upload/v1/finflow/test/public-id.pdf")
                .format("pdf")
                .fileSize(1234L)
                .originalFileName("salary-slip.pdf")
                .build();
        Document savedDocument = buildDocument(
                1L,
                15L,
                "applicant@finflow.com",
                "salary-slip.pdf",
                DocumentType.SALARY_SLIP,
                DocumentStatus.PENDING
        );
        savedDocument.setCloudinaryPublicId(cloudinaryResponse.getPublicId());
        savedDocument.setCloudinaryUrl(cloudinaryResponse.getSecureUrl());
        savedDocument.setStoredFileName(cloudinaryResponse.getPublicId());
        savedDocument.setFilePath(cloudinaryResponse.getSecureUrl());
        savedDocument.setFileType("PDF");
        savedDocument.setFileSize(1234L);

        when(cloudinaryService.uploadFile(file, "applicant@finflow.com/15"))
                .thenReturn(cloudinaryResponse);
        when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);
        DocumentResponse response = documentService.uploadDocument(
                file,
                15L,
                DocumentType.SALARY_SLIP,
                "applicant@finflow.com",
                "Bearer valid-token"
        );

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("salary-slip.pdf", response.getFileName());
        assertEquals("PDF", response.getFileType());
        assertEquals("https://res.cloudinary.com/demo/image/upload/v1/finflow/test/public-id.pdf",
                response.getCloudinaryUrl());
        assertEquals(DocumentType.SALARY_SLIP, response.getDocumentType());

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(captor.capture());
        Document documentToSave = captor.getValue();
        assertEquals(15L, documentToSave.getApplicationId());
        assertEquals("applicant@finflow.com", documentToSave.getApplicantEmail());
        assertEquals("finflow/test/public-id", documentToSave.getCloudinaryPublicId());
        assertEquals("https://res.cloudinary.com/demo/image/upload/v1/finflow/test/public-id.pdf",
                documentToSave.getCloudinaryUrl());
        assertEquals("PDF", documentToSave.getFileType());

        verify(applicationStatusEventPublisher).publishStatusUpdate(15L, "DOCS_PENDING");
    }

    @Test
    void uploadDocument_ShouldThrowApiException_WhenCloudinaryResponseHasBlankFormat() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "content".getBytes()
        );
        CloudinaryResponse cloudinaryResponse = CloudinaryResponse.builder()
                .publicId("finflow/test/public-id")
                .secureUrl("https://res.cloudinary.com/demo/image/upload/v1/finflow/test/public-id.pdf")
                .format(" ")
                .fileSize(1234L)
                .originalFileName("document.pdf")
                .build();

        when(cloudinaryService.uploadFile(file, "applicant@finflow.com/20"))
                .thenReturn(cloudinaryResponse);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> documentService.uploadDocument(
                        file,
                        20L,
                        DocumentType.OTHER,
                        "applicant@finflow.com",
                        "Bearer valid-token"
                )
        );

        assertEquals("Unable to determine uploaded file type", exception.getMessage());
        verify(documentRepository, never()).save(any(Document.class));
        verify(applicationStatusEventPublisher, never()).publishStatusUpdate(any(Long.class), any(String.class));
    }

    @Test
    void getMyDocuments_ShouldReturnMappedDocumentsForApplicant() {
        Document firstDocument = buildDocument(
                1L,
                15L,
                "applicant@finflow.com",
                "aadhar.pdf",
                DocumentType.AADHAR,
                DocumentStatus.PENDING
        );
        Document secondDocument = buildDocument(
                2L,
                16L,
                "applicant@finflow.com",
                "pan.png",
                DocumentType.PAN_CARD,
                DocumentStatus.VERIFIED
        );

        when(documentRepository.findByApplicantEmail("applicant@finflow.com"))
                .thenReturn(List.of(firstDocument, secondDocument));

        List<DocumentResponse> responses = documentService.getMyDocuments("applicant@finflow.com");

        assertEquals(2, responses.size());
        assertEquals("aadhar.pdf", responses.get(0).getFileName());
        assertEquals(DocumentType.PAN_CARD, responses.get(1).getDocumentType());
        verify(documentRepository).findByApplicantEmail("applicant@finflow.com");
    }

    @Test
    void getDocumentsByApplication_ShouldReturnDocumentsForGivenApplicationAndEmail() {
        Document document = buildDocument(
                3L,
                25L,
                "applicant@finflow.com",
                "bank-statement.pdf",
                DocumentType.BANK_STATEMENT,
                DocumentStatus.PENDING
        );

        when(documentRepository.findByApplicationIdAndApplicantEmail(25L, "applicant@finflow.com"))
                .thenReturn(List.of(document));

        List<DocumentResponse> responses = documentService.getDocumentsByApplication(25L, "applicant@finflow.com");

        assertEquals(1, responses.size());
        assertEquals(25L, responses.get(0).getApplicationId());
        assertEquals("bank-statement.pdf", responses.get(0).getFileName());
        verify(documentRepository).findByApplicationIdAndApplicantEmail(25L, "applicant@finflow.com");
    }

    @Test
    void getPendingDocuments_ShouldReturnAllPendingDocuments() {
        Document firstPendingDocument = buildDocument(
                4L,
                26L,
                "applicant1@finflow.com",
                "pending-aadhar.pdf",
                DocumentType.AADHAR,
                DocumentStatus.PENDING
        );
        Document secondPendingDocument = buildDocument(
                5L,
                27L,
                "applicant2@finflow.com",
                "pending-pan.pdf",
                DocumentType.PAN_CARD,
                DocumentStatus.PENDING
        );

        when(documentRepository.findByStatus(DocumentStatus.PENDING))
                .thenReturn(List.of(firstPendingDocument, secondPendingDocument));

        List<DocumentResponse> responses = documentService.getPendingDocuments();

        assertEquals(2, responses.size());
        assertEquals("pending-aadhar.pdf", responses.get(0).getFileName());
        assertEquals("pending-pan.pdf", responses.get(1).getFileName());
        assertEquals(DocumentStatus.PENDING, responses.get(0).getStatus());
        verify(documentRepository).findByStatus(DocumentStatus.PENDING);
    }

    @Test
    void verifyDocument_ShouldMarkRejectedDocument_AndMoveApplicationBackToDocsPending() {
        VerifyRequest request = new VerifyRequest();
        request.setStatus(DocumentStatus.REJECTED);
        request.setRemarks("Image is unclear");

        Document document = buildDocument(
                10L,
                30L,
                "applicant@finflow.com",
                "bank.pdf",
                DocumentType.BANK_STATEMENT,
                DocumentStatus.PENDING
        );
        Document savedDocument = buildDocument(
                10L,
                30L,
                "applicant@finflow.com",
                "bank.pdf",
                DocumentType.BANK_STATEMENT,
                DocumentStatus.REJECTED
        );
        savedDocument.setRemarks("Image is unclear");
        savedDocument.setVerifiedBy("admin@finflow.com");
        savedDocument.setVerifiedAt(LocalDateTime.now());

        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);
        when(documentRepository.findByApplicationId(30L)).thenReturn(List.of(savedDocument));
        DocumentResponse response = documentService.verifyDocument(
                10L,
                request,
                "admin@finflow.com",
                "Bearer admin-token"
        );

        assertEquals(DocumentStatus.REJECTED, response.getStatus());
        assertEquals("admin@finflow.com", response.getVerifiedBy());
        assertEquals("Image is unclear", response.getRemarks());
        assertNotNull(response.getVerifiedAt());

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(captor.capture());
        Document updatedDocument = captor.getValue();
        assertEquals(DocumentStatus.REJECTED, updatedDocument.getStatus());
        assertEquals("admin@finflow.com", updatedDocument.getVerifiedBy());
        assertEquals("Image is unclear", updatedDocument.getRemarks());
        assertNotNull(updatedDocument.getVerifiedAt());

        verify(applicationStatusEventPublisher).publishStatusUpdate(30L, "DOCS_PENDING");
    }

    @Test
    void verifyDocument_ShouldMarkApplicationDocsVerified_WhenAllDocumentsAreVerified() {
        VerifyRequest request = new VerifyRequest();
        request.setStatus(DocumentStatus.VERIFIED);
        request.setRemarks("Looks good");

        Document document = buildDocument(
                11L,
                40L,
                "applicant@finflow.com",
                "itr.pdf",
                DocumentType.ITR,
                DocumentStatus.PENDING
        );
        Document verifiedDocument = buildDocument(
                11L,
                40L,
                "applicant@finflow.com",
                "itr.pdf",
                DocumentType.ITR,
                DocumentStatus.VERIFIED
        );
        verifiedDocument.setRemarks("Looks good");
        verifiedDocument.setVerifiedBy("admin@finflow.com");
        verifiedDocument.setVerifiedAt(LocalDateTime.now());
        Document anotherVerifiedDocument = buildDocument(
                12L,
                40L,
                "applicant@finflow.com",
                "pan.pdf",
                DocumentType.PAN_CARD,
                DocumentStatus.VERIFIED
        );

        when(documentRepository.findById(11L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(verifiedDocument);
        when(documentRepository.findByApplicationId(40L))
                .thenReturn(List.of(verifiedDocument, anotherVerifiedDocument));
        DocumentResponse response = documentService.verifyDocument(
                11L,
                request,
                "admin@finflow.com",
                "Bearer admin-token"
        );

        assertEquals(DocumentStatus.VERIFIED, response.getStatus());
        assertEquals("Looks good", response.getRemarks());
        verify(applicationStatusEventPublisher).publishStatusUpdate(40L, "DOCS_VERIFIED");
    }

    @Test
    void verifyDocument_ShouldThrowApiException_WhenDocumentDoesNotExist() {
        VerifyRequest request = new VerifyRequest();
        request.setStatus(DocumentStatus.VERIFIED);

        when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> documentService.verifyDocument(
                        999L,
                        request,
                        "admin@finflow.com",
                        "Bearer admin-token"
                )
        );

        assertTrue(exception.getMessage().contains("Document not found: 999"));
        verify(documentRepository, never()).save(any(Document.class));
        verify(applicationStatusEventPublisher, never()).publishStatusUpdate(any(Long.class), any(String.class));
    }

    private Document buildDocument(Long id,
                                   Long applicationId,
                                   String email,
                                   String fileName,
                                   DocumentType documentType,
                                   DocumentStatus status) {
        Document document = Document.builder()
                .id(id)
                .applicationId(applicationId)
                .applicantEmail(email)
                .fileName(fileName)
                .storedFileName("stored-" + id)
                .filePath("https://res.cloudinary.com/demo/image/upload/v1/file-" + id + ".pdf")
                .cloudinaryPublicId("finflow/file-" + id)
                .cloudinaryUrl("https://res.cloudinary.com/demo/image/upload/v1/file-" + id + ".pdf")
                .fileType("PDF")
                .fileSize(2048L)
                .documentType(documentType)
                .status(status)
                .uploadedAt(LocalDateTime.now().minusDays(1))
                .verifiedAt(status == DocumentStatus.PENDING ? null : LocalDateTime.now())
                .verifiedBy(status == DocumentStatus.PENDING ? null : "admin@finflow.com")
                .remarks(status == DocumentStatus.PENDING ? null : "verified")
                .build();
        return document;
    }
}
