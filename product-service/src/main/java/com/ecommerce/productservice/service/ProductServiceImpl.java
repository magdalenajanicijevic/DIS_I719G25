package com.ecommerce.productservice.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ecommerce.productservice.dto.CreateProductRequest;
import com.ecommerce.productservice.dto.ProductResponse;
import com.ecommerce.productservice.dto.UpdateProductRequest;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.exception.ForbiddenException;
import com.ecommerce.productservice.exception.ProductAlreadyExistsException;
import com.ecommerce.productservice.exception.ProductNotFoundException;
import com.ecommerce.productservice.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

	private final ProductRepository productRepository;

	@Override
	public ProductResponse createProduct(CreateProductRequest request, String currentUserRole) {

		if (!"ADMIN".equals(currentUserRole)) {
			throw new ForbiddenException("Only administrators can create products.");
		}

		if (productRepository.existsByName(request.getName())) {
			throw new ProductAlreadyExistsException("Product already exists.");
		}

		Product product = Product.builder().name(request.getName()).description(request.getDescription())
				.price(request.getPrice()).category(request.getCategory()).build();

		Product savedProduct = productRepository.save(product);

		return mapToResponse(savedProduct);
	}

	@Override
	public ProductResponse getProductById(Long id) {

		Product product = productRepository.findByIdAndActiveTrue(id)
				.orElseThrow(() -> new ProductNotFoundException("Product with id " + id + " not found."));

		return mapToResponse(product);
	}

	@Override
	public ProductResponse getProductByIdIncludingInactive(Long id) {

		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ProductNotFoundException("Product with id " + id + " not found."));

		return mapToResponse(product);
	}

	@Override
	public List<ProductResponse> getAllProducts() {

		return productRepository.findByActiveTrue().stream().map(this::mapToResponse).toList();
	}

	@Override
	public List<ProductResponse> getAllProductsForAdmin(String currentUserRole) {

		if (!"ADMIN".equals(currentUserRole)) {
			throw new ForbiddenException("Only administrators can access all products.");
		}

		return productRepository.findAll().stream().map(this::mapToResponse).toList();
	}

	@Override
	public ProductResponse updateProduct(Long id, UpdateProductRequest request, String currentUserRole) {

		if (!"ADMIN".equals(currentUserRole)) {
			throw new ForbiddenException("Only administrators can update products.");
		}

		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ProductNotFoundException("Product with id " + id + " not found."));

		if (!product.getName().equals(request.getName()) && productRepository.existsByName(request.getName())) {

			throw new ProductAlreadyExistsException("Product with name '" + request.getName() + "' already exists.");
		}

		product.setName(request.getName());
		product.setDescription(request.getDescription());
		product.setPrice(request.getPrice());
		product.setCategory(request.getCategory());
		product.setActive(request.getActive());

		Product updatedProduct = productRepository.save(product);

		return mapToResponse(updatedProduct);
	}

	@Override
	public void deleteProduct(Long id, String currentUserRole) {

		if (!"ADMIN".equals(currentUserRole)) {
			throw new ForbiddenException("Only administrators can delete products.");
		}

		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ProductNotFoundException("Product with id " + id + " not found."));

		product.setActive(false);

		productRepository.save(product);
	}

	private ProductResponse mapToResponse(Product product) {

		return ProductResponse.builder().id(product.getId()).name(product.getName())
				.description(product.getDescription()).price(product.getPrice()).category(product.getCategory())
				.active(product.isActive()).createdAt(product.getCreatedAt()).updatedAt(product.getUpdatedAt()).build();
	}
}