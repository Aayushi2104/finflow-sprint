package com.finflow.document_service.storage;

public record StoredDocumentFile(
        String originalFileName,
        String storedFileName,
        String filePath,
        String fileExtension,
        long fileSize
) {
}
