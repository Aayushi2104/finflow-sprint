package com.finflow.application_service.service;

import com.finflow.application_service.dto.ApplicationRequest;
import com.finflow.application_service.dto.ApplicationResponse;
import com.finflow.application_service.dto.StatusResponse;
import com.finflow.application_service.entity.LoanApplication;
import com.finflow.application_service.entity.LoanApplication.ApplicationStatus;
import com.finflow.application_service.exception.ApiException;
import com.finflow.application_service.repository.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final LoanApplicationRepository repository;
    private final ModelMapper modelMapper;
    private final CacheManager cacheManager;

    @Override
    public ApplicationResponse createApplication(ApplicationRequest request, String email) {
        LoanApplication app = modelMapper.map(request, LoanApplication.class);
        app.setApplicantEmail(email);
        LoanApplication saved = repository.save(app);
        evictApplicationCaches(saved.getId(), saved.getApplicantEmail());
        return toResponse(saved);
    }

    @Override
    public ApplicationResponse updateApplication(Long id, ApplicationRequest request, String email) {
        LoanApplication app = getOwnedApplication(id, email);

        if (app.getStatus() != ApplicationStatus.DRAFT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only DRAFT applications can be updated");
        }

        modelMapper.map(request, app);
        LoanApplication saved = repository.save(app);
        evictApplicationCaches(saved.getId(), saved.getApplicantEmail());
        return toResponse(saved);
    }

    @Override
    public ApplicationResponse submitApplication(Long id, String email) {
        LoanApplication app = getOwnedApplication(id, email);

        if (app.getStatus() != ApplicationStatus.DRAFT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only DRAFT applications can be submitted");
        }

        app.setStatus(ApplicationStatus.SUBMITTED);
        app.setSubmittedAt(LocalDateTime.now());

        LoanApplication saved = repository.save(app);
        evictApplicationCaches(saved.getId(), saved.getApplicantEmail());
        return toResponse(saved);
    }

    @Override
    @Cacheable(cacheNames = "application.byOwner", key = "#email")
    public List<ApplicationResponse> getMyApplications(String email) {
        log.info("CACHE MISS - loading applications for applicant {}", email);
        return repository.findByApplicantEmailOrderByCreatedAtDesc(email)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(cacheNames = "application.byIdAndOwner", key = "#id + ':' + #email")
    public ApplicationResponse getApplication(Long id, String email) {
        log.info("CACHE MISS - loading application {} for applicant {}", id, email);
        return toResponse(getOwnedApplication(id, email));
    }

    @Override
    @Cacheable(cacheNames = "application.statusByIdAndOwner", key = "#id + ':' + #email")
    public StatusResponse getStatus(Long id, String email) {
        log.info("CACHE MISS - loading status for application {} and applicant {}", id, email);
        LoanApplication app = getOwnedApplication(id, email);

        return StatusResponse.builder()
                .applicationId(app.getId())
                .currentStatus(app.getStatus())
                .createdAt(app.getCreatedAt())
                .submittedAt(app.getSubmittedAt())
                .updatedAt(app.getUpdatedAt())
                .message(getStatusMessage(app.getStatus()))
                .build();
    }

    @Override
    @Cacheable(cacheNames = "application.all")
    public List<ApplicationResponse> getAllApplications() {
        log.info("CACHE MISS - loading all applications for admin view");
        return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ApplicationResponse updateStatus(Long id, ApplicationStatus newStatus) {
        LoanApplication app = repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found: " + id));
        app.setStatus(newStatus);
        LoanApplication saved = repository.save(app);
        evictApplicationCaches(saved.getId(), saved.getApplicantEmail());
        return toResponse(saved);
    }

    private LoanApplication getOwnedApplication(Long id, String email) {
        LoanApplication app = repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found: " + id));
        if (!app.getApplicantEmail().equals(email)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You don't have permission to access this application");
        }
        return app;
    }

    private String getStatusMessage(ApplicationStatus status) {
        return switch (status) {
            case DRAFT -> "Your application is saved as draft";
            case SUBMITTED -> "Application submitted, awaiting document upload";
            case DOCS_PENDING -> "Please upload your KYC and income documents";
            case DOCS_VERIFIED -> "Documents verified, application under review";
            case UNDER_REVIEW -> "Admin is reviewing your application";
            case APPROVED -> "Congratulations! Your loan is approved";
            case REJECTED -> "Your application has been rejected";
            case CLOSED -> "Application closed";
        };
    }

    private ApplicationResponse toResponse(LoanApplication app) {
        return modelMapper.map(app, ApplicationResponse.class);
    }

    private void evictApplicationCaches(Long id, String email) {
        evictCacheEntry("application.byOwner", email);
        evictCacheEntry("application.byIdAndOwner", id + ":" + email);
        evictCacheEntry("application.statusByIdAndOwner", id + ":" + email);
        clearCache("application.all");
    }

    private void evictCacheEntry(String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
