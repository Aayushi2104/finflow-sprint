package com.finflow.document_service.service;

import com.finflow.document_service.config.RabbitConfig;
import com.finflow.document_service.dto.ApplicationStatusUpdateEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationStatusEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Retry(name = "rabbitPublisher")
    @CircuitBreaker(name = "rabbitPublisher", fallbackMethod = "publishStatusUpdateFallback")
    public void publishStatusUpdate(Long applicationId, String status) {
        ApplicationStatusUpdateEvent event = new ApplicationStatusUpdateEvent(applicationId, status);
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.STATUS_ROUTING_KEY, event);
        log.info("Published RabbitMQ status update for application {} to {}", applicationId, status);
    }

    @SuppressWarnings("unused")
    private void publishStatusUpdateFallback(Long applicationId, String status, Throwable throwable) {
        log.error(
                "Failed to publish RabbitMQ status update for application {} to {}: {}",
                applicationId,
                status,
                throwable.getMessage(),
                throwable
        );
        throw new com.finflow.document_service.exception.ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Could not publish status update for application: " + applicationId
        );
    }
}
