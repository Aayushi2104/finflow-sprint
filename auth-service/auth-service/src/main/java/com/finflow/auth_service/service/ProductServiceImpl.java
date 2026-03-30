package com.finflow.auth_service.service;

import com.finflow.auth_service.dto.ProductResponse;
import com.finflow.auth_service.entity.LoanProduct;
import com.finflow.auth_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "auth.products")
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Cacheable(key = "'active'")
    public List<ProductResponse> getAllProducts() {
        List<LoanProduct> products = productRepository.findByActiveTrue();

        if (products.isEmpty()) {
            return getDefaultProducts();
        }

        return products.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private List<ProductResponse> getDefaultProducts() {
        return List.of(
                ProductResponse.builder()
                        .id(1L)
                        .productName("Home Loan")
                        .description("Finance your dream home with easy EMIs")
                        .minAmount(new BigDecimal("500000"))
                        .maxAmount(new BigDecimal("10000000"))
                        .minTenureMonths(12)
                        .maxTenureMonths(240)
                        .interestRate(new BigDecimal("8.5"))
                        .eligibility("Salaried or Self-employed, Age 21-65")
                        .build(),

                ProductResponse.builder()
                        .id(2L)
                        .productName("Personal Loan")
                        .description("Quick loans for personal needs")
                        .minAmount(new BigDecimal("50000"))
                        .maxAmount(new BigDecimal("2000000"))
                        .minTenureMonths(6)
                        .maxTenureMonths(60)
                        .interestRate(new BigDecimal("12.5"))
                        .eligibility("Salaried, minimum income 25000/month")
                        .build(),

                ProductResponse.builder()
                        .id(3L)
                        .productName("Business Loan")
                        .description("Grow your business with our funding")
                        .minAmount(new BigDecimal("200000"))
                        .maxAmount(new BigDecimal("5000000"))
                        .minTenureMonths(12)
                        .maxTenureMonths(84)
                        .interestRate(new BigDecimal("14.0"))
                        .eligibility("Business owners with 2+ years of operation")
                        .build(),

                ProductResponse.builder()
                        .id(4L)
                        .productName("Education Loan")
                        .description("Invest in your future with education funding")
                        .minAmount(new BigDecimal("100000"))
                        .maxAmount(new BigDecimal("3000000"))
                        .minTenureMonths(12)
                        .maxTenureMonths(120)
                        .interestRate(new BigDecimal("10.0"))
                        .eligibility("Students with admission letter from recognized institution")
                        .build()
        );
    }

    private ProductResponse toResponse(LoanProduct p) {
        return ProductResponse.builder()
                .id(p.getId())
                .productName(p.getProductName())
                .description(p.getDescription())
                .minAmount(p.getMinAmount())
                .maxAmount(p.getMaxAmount())
                .minTenureMonths(p.getMinTenureMonths())
                .maxTenureMonths(p.getMaxTenureMonths())
                .interestRate(p.getInterestRate())
                .eligibility(p.getEligibility())
                .build();
    }
}
