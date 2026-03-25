package com.finflow.document_service.storage;

import com.finflow.document_service.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Component
public class DocumentFilePolicy {

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("pdf", "jpg", "jpeg", "png");

    public String validateAndExtractExtension(MultipartFile file) {
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
}
