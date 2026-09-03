package com.ecommerce.productservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.productservice.dto.CreateProductRequest;
import com.ecommerce.productservice.dto.ProductResponse;
import com.ecommerce.productservice.dto.UpdateProductRequest;
import com.ecommerce.productservice.entity.Category;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.exception.ForbiddenException;
import com.ecommerce.productservice.exception.ProductAlreadyExistsException;
import com.ecommerce.productservice.exception.ProductNotFoundException;
import com.ecommerce.productservice.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

	@Mock
	private ProductRepository productRepository;

	@InjectMocks
	private ProductServiceImpl productService;

	private Product product;
	private CreateProductRequest createRequest;
	private UpdateProductRequest updateRequest;

	@BeforeEach
	void setUp() {

		product = Product.builder().id(1L).name("Laptop").description("Business laptop").price(new BigDecimal("999.99"))
				.category(Category.ELECTRONICS).active(true).build();

		createRequest = CreateProductRequest.builder().name("Laptop").description("Business laptop")
				.price(new BigDecimal("999.99")).category(Category.ELECTRONICS).build();

		updateRequest = UpdateProductRequest.builder().name("Updated Laptop").description("Updated description")
				.price(new BigDecimal("1099.99")).category(Category.ELECTRONICS).active(true).build();
	}

	@Test
	void createProduct_shouldCreateProduct_whenAdmin() {

		when(productRepository.existsByName(createRequest.getName())).thenReturn(false);
		when(productRepository.save(any(Product.class))).thenReturn(product);

		ProductResponse response = productService.createProduct(createRequest, "ADMIN");

		assertNotNull(response);
		assertEquals(product.getId(), response.getId());
		assertEquals(product.getName(), response.getName());
		assertEquals(product.getDescription(), response.getDescription());
		assertEquals(product.getPrice(), response.getPrice());
		assertEquals(product.getCategory(), response.getCategory());

		verify(productRepository).existsByName(createRequest.getName());
		verify(productRepository).save(any(Product.class));
	}

	@Test
	void createProduct_shouldThrowForbidden_whenCustomer() {

		assertThrows(ForbiddenException.class, () -> productService.createProduct(createRequest, "CUSTOMER"));

		verify(productRepository, never()).existsByName(any());
		verify(productRepository, never()).save(any(Product.class));
	}

	@Test
	void createProduct_shouldThrowException_whenProductAlreadyExists() {

		when(productRepository.existsByName(createRequest.getName())).thenReturn(true);

		ProductAlreadyExistsException exception = assertThrows(ProductAlreadyExistsException.class,
				() -> productService.createProduct(createRequest, "ADMIN"));

		assertEquals("Product already exists.", exception.getMessage());

		verify(productRepository).existsByName(createRequest.getName());
		verify(productRepository, never()).save(any(Product.class));
	}

	@Test
	void createProduct_shouldSaveCorrectProductData() {

		when(productRepository.existsByName(createRequest.getName())).thenReturn(false);

		when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

		productService.createProduct(createRequest, "ADMIN");

		ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);

		verify(productRepository).save(productCaptor.capture());

		Product savedProduct = productCaptor.getValue();

		assertEquals(createRequest.getName(), savedProduct.getName());
		assertEquals(createRequest.getDescription(), savedProduct.getDescription());
		assertEquals(createRequest.getPrice(), savedProduct.getPrice());
		assertEquals(createRequest.getCategory(), savedProduct.getCategory());
	}

	@Test
	void getProductById_shouldReturnProduct() {

		when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(product));

		ProductResponse response = productService.getProductById(1L);

		assertNotNull(response);
		assertEquals(product.getId(), response.getId());
		assertEquals(product.getName(), response.getName());
		assertEquals(product.getDescription(), response.getDescription());
		assertEquals(product.getPrice(), response.getPrice());
		assertEquals(product.getCategory(), response.getCategory());
		assertEquals(product.isActive(), response.isActive());

		verify(productRepository).findByIdAndActiveTrue(1L);
	}

	@Test
	void getProductById_shouldThrowException_whenProductDoesNotExist() {

		when(productRepository.findByIdAndActiveTrue(999L)).thenReturn(Optional.empty());

		ProductNotFoundException exception = assertThrows(ProductNotFoundException.class,
				() -> productService.getProductById(999L));

		assertEquals("Product with id 999 not found.", exception.getMessage());

		verify(productRepository).findByIdAndActiveTrue(999L);
	}

	@Test
	void getProductByIdIncludingInactive_shouldReturnProduct() {

		Product inactiveProduct = Product.builder().id(2L).name("Old Laptop").description("Inactive product")
				.price(new BigDecimal("499.99")).category(Category.ELECTRONICS).active(false).build();

		when(productRepository.findById(2L)).thenReturn(Optional.of(inactiveProduct));

		ProductResponse response = productService.getProductByIdIncludingInactive(2L);

		assertNotNull(response);
		assertEquals(2L, response.getId());
		assertEquals("Old Laptop", response.getName());
		assertEquals(false, response.isActive());

		verify(productRepository).findById(2L);
	}

	@Test
	void getProductByIdIncludingInactive_shouldThrowException_whenProductDoesNotExist() {

		when(productRepository.findById(999L)).thenReturn(Optional.empty());

		ProductNotFoundException exception = assertThrows(ProductNotFoundException.class,
				() -> productService.getProductByIdIncludingInactive(999L));

		assertEquals("Product with id 999 not found.", exception.getMessage());

		verify(productRepository).findById(999L);
	}

	@Test
	void getAllProducts_shouldReturnOnlyActiveProducts() {

		Product secondProduct = Product.builder().id(2L).name("Phone").description("Smartphone")
				.price(new BigDecimal("699.99")).category(Category.ELECTRONICS).active(true).build();

		when(productRepository.findByActiveTrue()).thenReturn(List.of(product, secondProduct));

		List<ProductResponse> response = productService.getAllProducts();

		assertNotNull(response);
		assertEquals(2, response.size());
		assertEquals("Laptop", response.get(0).getName());
		assertEquals("Phone", response.get(1).getName());

		verify(productRepository).findByActiveTrue();
	}

	@Test
	void getAllProducts_shouldReturnEmptyList_whenNoActiveProductsExist() {

		when(productRepository.findByActiveTrue()).thenReturn(List.of());

		List<ProductResponse> response = productService.getAllProducts();

		assertNotNull(response);
		assertEquals(0, response.size());

		verify(productRepository).findByActiveTrue();
	}

	@Test
	void getAllProductsForAdmin_shouldReturnAllProducts_whenAdmin() {

		Product inactiveProduct = Product.builder().id(2L).name("Old Phone").description("Inactive phone")
				.price(new BigDecimal("299.99")).category(Category.ELECTRONICS).active(false).build();

		when(productRepository.findAll()).thenReturn(List.of(product, inactiveProduct));

		List<ProductResponse> response = productService.getAllProductsForAdmin("ADMIN");

		assertNotNull(response);
		assertEquals(2, response.size());

		verify(productRepository).findAll();
	}

	@Test
	void getAllProductsForAdmin_shouldThrowForbidden_whenCustomer() {

		assertThrows(ForbiddenException.class, () -> productService.getAllProductsForAdmin("CUSTOMER"));

		verify(productRepository, never()).findAll();
	}

	@Test
	void updateProduct_shouldUpdateProduct_whenAdmin() {

		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		when(productRepository.existsByName(updateRequest.getName())).thenReturn(false);

		when(productRepository.save(any(Product.class))).thenReturn(product);

		ProductResponse response = productService.updateProduct(1L, updateRequest, "ADMIN");

		assertNotNull(response);

		verify(productRepository).findById(1L);
		verify(productRepository).existsByName(updateRequest.getName());
		verify(productRepository).save(product);
	}

	@Test
	void updateProduct_shouldThrowForbidden_whenCustomer() {

		assertThrows(ForbiddenException.class, () -> productService.updateProduct(1L, updateRequest, "CUSTOMER"));

		verify(productRepository, never()).findById(any());
		verify(productRepository, never()).save(any(Product.class));
	}

	@Test
	void updateProduct_shouldThrowException_whenProductDoesNotExist() {

		when(productRepository.findById(999L)).thenReturn(Optional.empty());

		ProductNotFoundException exception = assertThrows(ProductNotFoundException.class,
				() -> productService.updateProduct(999L, updateRequest, "ADMIN"));

		assertEquals("Product with id 999 not found.", exception.getMessage());

		verify(productRepository).findById(999L);
		verify(productRepository, never()).save(any(Product.class));
	}

	@Test
	void updateProduct_shouldThrowException_whenNewNameAlreadyExists() {

		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		when(productRepository.existsByName(updateRequest.getName())).thenReturn(true);

		ProductAlreadyExistsException exception = assertThrows(ProductAlreadyExistsException.class,
				() -> productService.updateProduct(1L, updateRequest, "ADMIN"));

		assertEquals("Product with name 'Updated Laptop' already exists.", exception.getMessage());

		verify(productRepository).findById(1L);
		verify(productRepository).existsByName(updateRequest.getName());
		verify(productRepository, never()).save(any(Product.class));
	}

	@Test
	void updateProduct_shouldAllowSameName() {

		UpdateProductRequest request = UpdateProductRequest.builder().name("Laptop").description("Updated description")
				.price(new BigDecimal("1099.99")).category(Category.ELECTRONICS).active(true).build();

		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		when(productRepository.save(any(Product.class))).thenReturn(product);

		productService.updateProduct(1L, request, "ADMIN");

		verify(productRepository).findById(1L);
		verify(productRepository, never()).existsByName(any());
		verify(productRepository).save(product);
	}

	@Test
	void deleteProduct_shouldDeactivateProduct_whenAdmin() {

		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		productService.deleteProduct(1L, "ADMIN");

		assertEquals(false, product.isActive());

		verify(productRepository).findById(1L);
		verify(productRepository).save(product);
	}

	@Test
	void deleteProduct_shouldThrowForbidden_whenCustomer() {

		assertThrows(ForbiddenException.class, () -> productService.deleteProduct(1L, "CUSTOMER"));

		verify(productRepository, never()).findById(any());
		verify(productRepository, never()).save(any(Product.class));
	}

	@Test
	void deleteProduct_shouldThrowException_whenProductDoesNotExist() {

		when(productRepository.findById(999L)).thenReturn(Optional.empty());

		ProductNotFoundException exception = assertThrows(ProductNotFoundException.class,
				() -> productService.deleteProduct(999L, "ADMIN"));

		assertEquals("Product with id 999 not found.", exception.getMessage());

		verify(productRepository).findById(999L);
		verify(productRepository, never()).save(any(Product.class));
	}
}