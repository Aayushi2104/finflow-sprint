package com.finflow.auth_service.repository;

import com.finflow.auth_service.entity.LoanProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<LoanProduct, Long> {
    List<LoanProduct> findByActiveTrue();
}