package com.finflow.document_service.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalDocumentStorageService implements DocumentStorageService {

    private final DocumentFilePolicy documentFilePolicy;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public StoredDocumentFile store(MultipartFile file) throws IOException {
        String extension = documentFilePolicy.validateAndExtractExtension(file);
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

        return new StoredDocumentFile(
                originalFileName,
                storedFileName,
                filePath.toString(),
                extension,
                file.getSize()
        );
    }
}
