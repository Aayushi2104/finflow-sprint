package com.finflow.auth_service.controller;

import com.finflow.auth_service.dto.ProductResponse;
import com.finflow.auth_service.security.JwtAuthFilter;
import com.finflow.auth_service.security.JwtUtil;
import com.finflow.auth_service.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void getAllProducts_ShouldReturnProducts() throws Exception {
        ProductResponse response = ProductResponse.builder()
                .id(1L)
                .productName("Home Loan")
                .minAmount(new BigDecimal("100000"))
                .build();

        when(productService.getAllProducts()).thenReturn(List.of(response));

        mockMvc.perform(get("/auth/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productName").value("Home Loan"));
    }
}
