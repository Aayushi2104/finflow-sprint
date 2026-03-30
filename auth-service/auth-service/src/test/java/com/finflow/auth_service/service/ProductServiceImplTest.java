package com.finflow.auth_service.service;

import com.finflow.auth_service.dto.ProductResponse;
import com.finflow.auth_service.entity.LoanProduct;
import com.finflow.auth_service.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void getAllProducts_ShouldReturnActiveProductsFromRepository() {
        LoanProduct product = LoanProduct.builder()
                .id(1L)
                .productName("Home Loan")
                .description("Finance home")
                .minAmount(new BigDecimal("100000"))
                .maxAmount(new BigDecimal("1000000"))
                .minTenureMonths(12)
                .maxTenureMonths(120)
                .interestRate(new BigDecimal("8.5"))
                .eligibility("Salaried")
                .active(true)
                .build();

        when(productRepository.findByActiveTrue()).thenReturn(List.of(product));

        List<ProductResponse> responses = productService.getAllProducts();

        assertEquals(1, responses.size());
        assertEquals("Home Loan", responses.get(0).getProductName());
        verify(productRepository).findByActiveTrue();
    }

    @Test
    void getAllProducts_ShouldReturnDefaultProducts_WhenRepositoryIsEmpty() {
        when(productRepository.findByActiveTrue()).thenReturn(List.of());

        List<ProductResponse> responses = productService.getAllProducts();

        assertEquals(4, responses.size());
        assertEquals("Home Loan", responses.get(0).getProductName());
        assertFalse(responses.isEmpty());
    }
}
