package com.finflow.auth_service.service;

import com.finflow.auth_service.dto.AuthResposne;
import com.finflow.auth_service.dto.LoginRequest;
import com.finflow.auth_service.dto.SignUpRequest;
import com.finflow.auth_service.dto.UpdateUserRequest;
import com.finflow.auth_service.dto.UserResponse;
import com.finflow.auth_service.entity.User;
import com.finflow.auth_service.exception.ApiException;
import com.finflow.auth_service.repository.UserRepository;
import com.finflow.auth_service.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void signup_ShouldCreateApplicantAndReturnToken() {
        SignUpRequest request = new SignUpRequest();
        request.setFullName("Aayushi Jain");
        request.setEmail("aayushi@finflow.com");
        request.setPassword("secret123");

        User mappedUser = User.builder()
                .fullName("Aayushi Jain")
                .email("aayushi@finflow.com")
                .build();
        User savedUser = User.builder()
                .id(1L)
                .fullName("Aayushi Jain")
                .email("aayushi@finflow.com")
                .password("encoded-secret")
                .role(User.Role.APPLICANT)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();
        AuthResposne mappedResponse = AuthResposne.builder()
                .email("aayushi@finflow.com")
                .fullName("Aayushi Jain")
                .role("APPLICANT")
                .build();

        when(userRepository.existsByEmail("aayushi@finflow.com")).thenReturn(false);
        when(modelMapper.map(request, User.class)).thenReturn(mappedUser);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken("aayushi@finflow.com", "APPLICANT")).thenReturn("jwt-token");
        when(modelMapper.map(any(User.class), eq(AuthResposne.class))).thenReturn(mappedResponse);

        AuthResposne response = authService.signup(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("Registration successful", response.getMessage());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void signup_ShouldThrowConflict_WhenEmailAlreadyExists() {
        SignUpRequest request = new SignUpRequest();
        request.setEmail("existing@finflow.com");

        when(userRepository.existsByEmail("existing@finflow.com")).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> authService.signup(request));

        assertEquals("Email already registered", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_ShouldReturnAuthResponse_WhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@finflow.com");
        request.setPassword("secret123");

        User user = User.builder()
                .id(2L)
                .email("user@finflow.com")
                .fullName("Test User")
                .password("encoded-secret")
                .role(User.Role.APPLICANT)
                .enabled(true)
                .build();
        AuthResposne authResposne = AuthResposne.builder()
                .email("user@finflow.com")
                .fullName("Test User")
                .role("APPLICANT")
                .build();

        when(userRepository.findByEmail("user@finflow.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "encoded-secret")).thenReturn(true);
        when(jwtUtil.generateToken("user@finflow.com", "APPLICANT")).thenReturn("jwt-token");
        when(modelMapper.map(user, AuthResposne.class)).thenReturn(authResposne);

        AuthResposne response = authService.login(request);

        assertEquals("jwt-token", response.getToken());
        assertEquals("Login successful", response.getMessage());
    }

    @Test
    void login_ShouldThrowNotFound_WhenUserDoesNotExist() {
        LoginRequest request = new LoginRequest();
        request.setEmail("missing@finflow.com");
        request.setPassword("secret123");

        when(userRepository.findByEmail("missing@finflow.com")).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> authService.login(request));

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void login_ShouldThrowBadCredentials_WhenPasswordIsInvalid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@finflow.com");
        request.setPassword("wrong");

        User user = User.builder()
                .email("user@finflow.com")
                .password("encoded-secret")
                .role(User.Role.APPLICANT)
                .build();

        when(userRepository.findByEmail("user@finflow.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-secret")).thenReturn(false);

        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> authService.login(request));

        assertEquals("Invalid password", exception.getMessage());
    }

    @Test
    void createAdmin_ShouldCreateAdminUser() {
        SignUpRequest request = new SignUpRequest();
        request.setFullName("Admin User");
        request.setEmail("admin@finflow.com");
        request.setPassword("secret123");

        User savedUser = User.builder()
                .id(3L)
                .fullName("Admin User")
                .email("admin@finflow.com")
                .password("encoded-secret")
                .role(User.Role.ADMIN)
                .enabled(true)
                .build();
        AuthResposne authResposne = AuthResposne.builder()
                .email("admin@finflow.com")
                .fullName("Admin User")
                .role("ADMIN")
                .build();

        when(userRepository.existsByEmail("admin@finflow.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken("admin@finflow.com", "ADMIN")).thenReturn("admin-token");
        when(modelMapper.map(any(User.class), eq(AuthResposne.class))).thenReturn(authResposne);

        AuthResposne response = authService.createAdmin(request);

        assertEquals("admin-token", response.getToken());
        assertEquals("Admin created successfully", response.getMessage());
    }

    @Test
    void getAllUsers_ShouldMapAllUsers() {
        User firstUser = buildUser(1L, "user1@finflow.com", "User One", User.Role.APPLICANT, true);
        User secondUser = buildUser(2L, "admin@finflow.com", "Admin", User.Role.ADMIN, true);

        when(userRepository.findAll()).thenReturn(List.of(firstUser, secondUser));

        List<UserResponse> users = authService.getAllUsers();

        assertEquals(2, users.size());
        assertEquals("user1@finflow.com", users.get(0).getEmail());
        assertEquals("ADMIN", users.get(1).getRole());
    }

    @Test
    void updateUser_ShouldUpdateProvidedFieldsOnly() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Updated Name");
        request.setEnabled(false);

        User existingUser = buildUser(5L, "user@finflow.com", "Old Name", User.Role.APPLICANT, true);
        User savedUser = buildUser(5L, "user@finflow.com", "Updated Name", User.Role.APPLICANT, false);

        when(userRepository.findById(5L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = authService.updateUser(5L, request);

        assertEquals("Updated Name", response.getFullName());
        assertEquals(false, response.isEnabled());
    }

    @Test
    void updateUser_ShouldThrowNotFound_WhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class,
                () -> authService.updateUser(99L, new UpdateUserRequest()));

        assertEquals("User not found: 99", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    private User buildUser(Long id, String email, String fullName, User.Role role, boolean enabled) {
        return User.builder()
                .id(id)
                .email(email)
                .fullName(fullName)
                .password("encoded")
                .role(role)
                .enabled(enabled)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
