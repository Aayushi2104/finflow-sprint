package com.finflow.document_service.service;

import com.finflow.document_service.dto.CloudinaryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface CloudinaryService {

    CloudinaryResponse uploadFile(MultipartFile file, String folder) throws IOException;

    void deleteFile(String publicId);
}
