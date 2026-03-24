package com.finflow.auth_service.service;

import com.finflow.auth_service.dto.AuthResposne;
import com.finflow.auth_service.dto.LoginRequest;
import com.finflow.auth_service.dto.SignUpRequest;
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

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ModelMapper modelMapper;

    public AuthResposne signup(SignUpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT,"Email already registered");
        }

        User user = modelMapper.map(request, User.class);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.APPLICANT);
        user.setEnabled(true);

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        AuthResposne response = modelMapper.map(user, AuthResposne.class);
        response.setToken(token);
        response.setMessage("Registration successful");

        return response;
    }

    public AuthResposne login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"User not found"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        AuthResposne response = modelMapper.map(user, AuthResposne.class);
        response.setToken(token);
        response.setMessage("Login successful");

        return response;
    }
}
