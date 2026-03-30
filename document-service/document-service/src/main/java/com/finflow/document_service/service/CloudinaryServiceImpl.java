package com.finflow.document_service.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.finflow.document_service.dto.CloudinaryResponse;
import com.finflow.document_service.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Override
    @SuppressWarnings("unchecked")
    public CloudinaryResponse uploadFile(MultipartFile file, String folder) throws IOException {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        String originalFileName = file.getOriginalFilename();
        String extension = getExtension(originalFileName);

        if (!isAllowed(extension)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only PDF, JPG, PNG files allowed");
        }

        try {
            Map<String, Object> options = ObjectUtils.asMap(
                    "folder", "finflow/" + folder,
                    "resource_type", getResourceType(extension),
                    "use_filename", true,
                    "unique_filename", true
            );

            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), options);

            String publicId = (String) result.get("public_id");
            String format = (String) result.get("format");
            if (format == null || format.isBlank()) {
                format = extension;
            }
            String secureUrl = buildDeliveryUrl(publicId, format);
            Long bytes = ((Number) result.get("bytes")).longValue();

            log.info("File uploaded to Cloudinary: {}", secureUrl);

            return CloudinaryResponse.builder()
                    .publicId(publicId)
                    .secureUrl(secureUrl)
                    .format(format)
                    .fileSize(bytes)
                    .originalFileName(originalFileName)
                    .build();
        } catch (Exception e) {
            log.error("Cloudinary upload failed: {}", e.getMessage());
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "File upload failed: " + e.getMessage()
            );
        }
    }

    @Override
    public void deleteFile(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "auto"));
            log.info("File deleted from Cloudinary: {}", publicId);
        } catch (Exception e) {
            log.error("Cloudinary delete failed: {}", e.getMessage());
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private boolean isAllowed(String extension) {
        return List.of("pdf", "jpg", "jpeg", "png").contains(extension);
    }

    private String buildDeliveryUrl(String publicId, String format) {
        return cloudinary.url()
                .secure(true)
                .resourceType("image")
                .format(format)
                .generate(publicId);
    }

    private String getResourceType(String extension) {
        return "image";
    }
}
