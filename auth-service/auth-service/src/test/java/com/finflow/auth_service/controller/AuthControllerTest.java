package com.finflow.auth_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finflow.auth_service.dto.AuthResposne;
import com.finflow.auth_service.dto.SignUpRequest;
import com.finflow.auth_service.dto.UpdateUserRequest;
import com.finflow.auth_service.dto.UserResponse;
import com.finflow.auth_service.exception.GlobalExceptionHandler;
import com.finflow.auth_service.security.JwtAuthFilter;
import com.finflow.auth_service.security.JwtUtil;
import com.finflow.auth_service.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({AuthControllerTest.TestSecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void signup_ShouldReturnAuthResponse() throws Exception {
        SignUpRequest request = new SignUpRequest();
        request.setFullName("Aayushi Jain");
        request.setEmail("aayushi@finflow.com");
        request.setPassword("secret123");

        AuthResposne response = AuthResposne.builder()
                .email("aayushi@finflow.com")
                .fullName("Aayushi Jain")
                .role("APPLICANT")
                .token("jwt-token")
                .message("Registration successful")
                .build();

        when(authService.signup(any(SignUpRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.message").value("Registration successful"));
    }

    @Test
    void signup_ShouldReturnBadRequest_WhenPayloadIsInvalid() throws Exception {
        SignUpRequest request = new SignUpRequest();
        request.setFullName("");
        request.setEmail("bad-email");
        request.setPassword("123");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void login_ShouldReturnAuthResponse() throws Exception {
        String payload = """
                {"email":"aayushi@finflow.com","password":"secret123"}
                """;
        AuthResposne response = AuthResposne.builder()
                .email("aayushi@finflow.com")
                .token("jwt-token")
                .role("APPLICANT")
                .message("Login successful")
                .build();

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void createAdmin_ShouldReturnAuthResponse() throws Exception {
        SignUpRequest request = new SignUpRequest();
        request.setFullName("Admin User");
        request.setEmail("admin@finflow.com");
        request.setPassword("secret123");

        AuthResposne response = AuthResposne.builder()
                .email("admin@finflow.com")
                .role("ADMIN")
                .token("admin-token")
                .message("Admin created successfully")
                .build();

        when(authService.createAdmin(any(SignUpRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/create-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @WithMockUser(username = "admin@finflow.com", roles = "ADMIN")
    void getAllUsers_ShouldReturnUsers_WhenAdminIsAuthorized() throws Exception {
        UserResponse response = UserResponse.builder()
                .id(1L)
                .email("user@finflow.com")
                .fullName("User Name")
                .role("APPLICANT")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(authService.getAllUsers()).thenReturn(List.of(response));

        mockMvc.perform(get("/auth/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("user@finflow.com"));
    }

    @Test
    @WithMockUser(username = "user@finflow.com", roles = "APPLICANT")
    void getAllUsers_ShouldReturnForbidden_WhenApplicantAccessesAdminEndpoint() throws Exception {
        mockMvc.perform(get("/auth/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@finflow.com", roles = "ADMIN")
    void updateUser_ShouldReturnUpdatedUser_WhenAdminIsAuthorized() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Updated Name");
        request.setEnabled(false);

        UserResponse response = UserResponse.builder()
                .id(5L)
                .email("user@finflow.com")
                .fullName("Updated Name")
                .role("APPLICANT")
                .enabled(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(authService.updateUser(eq(5L), any(UpdateUserRequest.class))).thenReturn(response);

        mockMvc.perform(put("/auth/admin/users/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
}
