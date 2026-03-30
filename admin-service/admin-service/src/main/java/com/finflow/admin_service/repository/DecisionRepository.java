package com.finflow.admin_service.repository;

import com.finflow.admin_service.entity.Decision;
import com.finflow.admin_service.entity.Decision.DecisionType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DecisionRepository extends JpaRepository<Decision, Long> {
    Optional<Decision> findByApplicationId(Long applicationId);
    List<Decision> findByDecision(DecisionType decision);
    long countByDecision(DecisionType decision);
}
