package com.ecommerce.productservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.productservice.dto.CreateProductRequest;
import com.ecommerce.productservice.dto.ProductResponse;
import com.ecommerce.productservice.dto.UpdateProductRequest;
import com.ecommerce.productservice.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
@Tag(name = "Product Controller", description = "Operations related to products")
public class ProductController {

	private final ProductService productService;

	@PostMapping
	@Operation(summary = "Create a new product")
	public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request,
			@RequestHeader("X-User-Role") String currentUserRole) {

		ProductResponse response = productService.createProduct(request, currentUserRole);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get product by id")
	public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {

		return ResponseEntity.ok(productService.getProductById(id));
	}

	@GetMapping("/internal/{id}")
	@Operation(summary = "Internal - Get product by id")
	public ResponseEntity<ProductResponse> getProductForInternalUse(@PathVariable Long id) {

		return ResponseEntity.ok(productService.getProductByIdIncludingInactive(id));
	}

	@GetMapping
	@Operation(summary = "Get all active products")
	public ResponseEntity<List<ProductResponse>> getAllProducts() {

		return ResponseEntity.ok(productService.getAllProducts());
	}

	@GetMapping("/admin")
	@Operation(summary = "Get all products (Admin)")
	public ResponseEntity<List<ProductResponse>> getAllProductsForAdmin(
			@RequestHeader("X-User-Role") String currentUserRole) {

		return ResponseEntity.ok(productService.getAllProductsForAdmin(currentUserRole));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update product")
	public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id,
			@Valid @RequestBody UpdateProductRequest request, @RequestHeader("X-User-Role") String currentUserRole) {

		return ResponseEntity.ok(productService.updateProduct(id, request, currentUserRole));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Deactivate product (Soft Delete)")
	public ResponseEntity<Void> deleteProduct(@PathVariable Long id,
			@RequestHeader("X-User-Role") String currentUserRole) {

		productService.deleteProduct(id, currentUserRole);

		return ResponseEntity.noContent().build();
	}
}