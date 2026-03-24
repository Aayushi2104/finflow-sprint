package com.finflow.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignUpRequest {
    @NotBlank
    private String fullName;

    @NotBlank
    @Size(min = 6)
    private String password;

    @Email
    @NotBlank
    private String email;
}
