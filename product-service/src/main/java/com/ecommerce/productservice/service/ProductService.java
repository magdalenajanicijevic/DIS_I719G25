package com.ecommerce.productservice.service;

import java.util.List;

import com.ecommerce.productservice.dto.CreateProductRequest;
import com.ecommerce.productservice.dto.ProductResponse;
import com.ecommerce.productservice.dto.UpdateProductRequest;

public interface ProductService {

	ProductResponse createProduct(CreateProductRequest request, String currentUserRole);

	ProductResponse getProductById(Long id);

	List<ProductResponse> getAllProducts();

	List<ProductResponse> getAllProductsForAdmin(String currentUserRole);

	ProductResponse updateProduct(Long id, UpdateProductRequest request, String currentUserRole);

	void deleteProduct(Long id, String currentUserRole);

	ProductResponse getProductByIdIncludingInactive(Long id);
}