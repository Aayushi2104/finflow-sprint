package com.finflow.document_service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface DocumentStorageService {

    StoredDocumentFile store(MultipartFile file) throws IOException;
}
