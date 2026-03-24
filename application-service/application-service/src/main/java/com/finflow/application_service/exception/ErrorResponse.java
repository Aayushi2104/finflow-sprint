package com.finflow.application_service.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private String path;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    public static ErrorResponse of(HttpStatus httpStatus, String message, String path) {
        return ErrorResponse.builder()
                .status(httpStatus.value())
                .error(httpStatus.name())
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }

}
