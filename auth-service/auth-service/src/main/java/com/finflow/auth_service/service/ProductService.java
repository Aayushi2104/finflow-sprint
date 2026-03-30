package com.finflow.auth_service.service;

import com.finflow.auth_service.dto.ProductResponse;

import java.util.List;

public interface ProductService {
    List<ProductResponse> getAllProducts();
}
