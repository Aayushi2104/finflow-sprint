package com.finflow.admin_service.service;

import com.finflow.admin_service.client.ApplicationClient;
import com.finflow.admin_service.client.AuthClient;
import com.finflow.admin_service.dto.ApplicationResponse;
import com.finflow.admin_service.dto.UserResponse;
import com.finflow.admin_service.exception.ApiException;
import feign.FeignException;
import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminDownstreamService {

    private static final Logger log = LoggerFactory.getLogger(AdminDownstreamService.class);

    private final ApplicationClient applicationClient;
    private final AuthClient authClient;

    @Value("${services.application-service.url}")
    private String applicationServiceUrl;

    @Value("${services.auth-service.url}")
    private String authServiceUrl;

    public AdminDownstreamService(ApplicationClient applicationClient, AuthClient authClient) {
        this.applicationClient = applicationClient;
        this.authClient = authClient;
    }

    @Retry(name = "applicationService")
    @CircuitBreaker(name = "applicationService", fallbackMethod = "getAllApplicationsFallback")
    public List<ApplicationResponse> getAllApplications(String authToken) {
        return applicationClient.getAllApplications(authToken);
    }

    @Retry(name = "applicationService")
    @CircuitBreaker(name = "applicationService", fallbackMethod = "updateApplicationStatusFallback")
    public void updateApplicationStatus(Long applicationId, String status, String authToken) {
        applicationClient.updateStatus(applicationId, status, authToken);
    }

    @Retry(name = "authService")
    @CircuitBreaker(name = "authService", fallbackMethod = "getAllUsersFallback")
    public List<UserResponse> getAllUsers(String authToken) {
        return authClient.getAllUsers(authToken);
    }

    @SuppressWarnings("unused")
    private List<ApplicationResponse> getAllApplicationsFallback(String authToken, Throwable throwable) {
        throw mapApplicationFailure("fetch applications", throwable);
    }

    @SuppressWarnings("unused")
    private void updateApplicationStatusFallback(Long applicationId, String status, String authToken, Throwable throwable) {
        throw mapApplicationFailure("update application status", throwable);
    }

    @SuppressWarnings("unused")
    private List<UserResponse> getAllUsersFallback(String authToken, Throwable throwable) {
        throw mapAuthFailure("fetch users", throwable);
    }

    private ApiException mapApplicationFailure(String action, Throwable throwable) {
        if (throwable instanceof RetryableException) {
            log.error("Application service is unreachable: {}", throwable.getMessage());
            return new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Application service is unreachable at " + applicationServiceUrl
            );
        }
        if (throwable instanceof FeignException feignException) {
            return downstreamApiException(action, feignException);
        }
        log.error("Application service call failed while trying to {}: {}", action, throwable.getMessage(), throwable);
        return new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Could not " + action + ": " + throwable.getMessage()
        );
    }

    private ApiException mapAuthFailure(String action, Throwable throwable) {
        if (throwable instanceof RetryableException) {
            log.error("Auth service is unreachable: {}", throwable.getMessage());
            return new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Auth service is unreachable at " + authServiceUrl
            );
        }
        if (throwable instanceof FeignException feignException) {
            return downstreamApiException(action, feignException);
        }
        log.error("Auth service call failed while trying to {}: {}", action, throwable.getMessage(), throwable);
        return new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Could not " + action + ": " + throwable.getMessage()
        );
    }

    private ApiException downstreamApiException(String action, FeignException exception) {
        HttpStatus status = HttpStatus.resolve(exception.status());
        HttpStatus resolvedStatus = status != null ? status : HttpStatus.SERVICE_UNAVAILABLE;
        String body = exception.contentUTF8();
        String message = body == null || body.isBlank() ? exception.getMessage() : body;

        log.error(
                "Downstream error while trying to {}: status={} body={}",
                action,
                exception.status(),
                message
        );

        return new ApiException(resolvedStatus, "Failed to " + action + ": " + message);
    }
}
