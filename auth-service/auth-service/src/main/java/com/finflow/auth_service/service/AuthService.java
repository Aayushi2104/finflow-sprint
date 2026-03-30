package com.finflow.auth_service.service;

import com.finflow.auth_service.dto.AuthResposne;
import com.finflow.auth_service.dto.LoginRequest;
import com.finflow.auth_service.dto.SignUpRequest;
import com.finflow.auth_service.dto.UpdateUserRequest;
import com.finflow.auth_service.dto.UserResponse;
import com.finflow.auth_service.entity.User;

import java.util.List;

public interface AuthService {

    AuthResposne signup(SignUpRequest request);

    AuthResposne login(LoginRequest request);

    AuthResposne createAdmin(SignUpRequest request);

    List<UserResponse> getAllUsers();

    UserResponse updateUser(Long userId, UpdateUserRequest request);

    AuthResposne buildAuthResponse(User user, String message);
}
