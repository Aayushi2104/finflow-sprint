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
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ModelMapper modelMapper;

    @Override
    public AuthResposne signup(SignUpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = modelMapper.map(request, User.class);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.APPLICANT);
        user.setEnabled(true);

        userRepository.save(user);
        return buildAuthResponse(user, "Registration successful");
    }

    @Override
    public AuthResposne login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        return buildAuthResponse(user, "Login successful");
    }

    @Override
    public AuthResposne createAdmin(SignUpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.ADMIN)
                .enabled(true)
                .build();

        userRepository.save(user);
        return buildAuthResponse(user, "Admin created successfully");
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .role(user.getRole().name())
                        .enabled(user.isEnabled())
                        .createdAt(user.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public AuthResposne buildAuthResponse(User user, String message) {
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        AuthResposne response = modelMapper.map(user, AuthResposne.class);
        response.setToken(token);
        response.setMessage(message);
        return response;
    }

    @Override
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "User not found: " + userId));

        // Update only fields that are provided
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        User saved = userRepository.save(user);

        return UserResponse.builder()
                .id(saved.getId())
                .email(saved.getEmail())
                .fullName(saved.getFullName())
                .role(saved.getRole().name())
                .enabled(saved.isEnabled())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
