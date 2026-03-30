package com.finflow.application_service.listener;

import com.finflow.application_service.config.RabbitConfig;
import com.finflow.application_service.dto.ApplicationStatusUpdateEvent;
import com.finflow.application_service.entity.LoanApplication.ApplicationStatus;
import com.finflow.application_service.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationStatusListener {

    private final ApplicationService applicationService;

    @RabbitListener(queues = RabbitConfig.STATUS_QUEUE)
    public void handleStatusUpdate(ApplicationStatusUpdateEvent event) {
        ApplicationStatus status = ApplicationStatus.valueOf(event.getStatus());
        applicationService.updateStatus(event.getApplicationId(), status);
        log.info("Processed RabbitMQ status update for application {} to {}",
                event.getApplicationId(), status);
    }
}
