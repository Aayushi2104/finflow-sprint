package com.finflow.document_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudinaryResponse {

    private String publicId;
    private String secureUrl;
    private String format;
    private Long fileSize;
    private String originalFileName;
}
