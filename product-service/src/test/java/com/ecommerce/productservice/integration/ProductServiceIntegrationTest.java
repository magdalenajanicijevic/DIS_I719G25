package com.ecommerce.productservice.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerce.productservice.entity.Category;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.repository.ProductRepository;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProductServiceIntegrationTest {

	@SuppressWarnings("resource")
	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18").withDatabaseName("product_db")
			.withUsername("postgres").withPassword("postgres");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepository productRepository;

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {

		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
	}

	@BeforeEach
	void setUp() {
		productRepository.deleteAll();
	}

	@Test
	void createProduct_shouldCreateProductForAdmin() throws Exception {

		String request = """
				{
				    "name": "Laptop",
				    "description": "Business laptop",
				    "price": 999.99,
				    "category": "ELECTRONICS"
				}
				""";

		mockMvc.perform(post("/api/products").header("X-User-Role", "ADMIN").contentType(MediaType.APPLICATION_JSON)
				.content(request)).andExpect(status().isCreated());
	}

	@Test
	void createProduct_shouldRejectCustomer() throws Exception {

		String request = """
				{
				    "name": "Laptop",
				    "description": "Business laptop",
				    "price": 999.99,
				    "category": "ELECTRONICS"
				}
				""";

		mockMvc.perform(post("/api/products").header("X-User-Role", "CUSTOMER").contentType(MediaType.APPLICATION_JSON)
				.content(request)).andExpect(status().isForbidden());
	}

	@Test
	void createProduct_shouldRejectDuplicateName() throws Exception {

		String request = """
				{
				    "name": "Laptop",
				    "description": "Business laptop",
				    "price": 999.99,
				    "category": "ELECTRONICS"
				}
				""";

		mockMvc.perform(post("/api/products").header("X-User-Role", "ADMIN").contentType(MediaType.APPLICATION_JSON)
				.content(request)).andExpect(status().isCreated());

		mockMvc.perform(post("/api/products").header("X-User-Role", "ADMIN").contentType(MediaType.APPLICATION_JSON)
				.content(request)).andExpect(status().isConflict());
	}

	@Test
	void getProductById_shouldReturnProductForCustomer() throws Exception {

		Product product = createProduct("Laptop", "Business laptop", 999.99, true);

		mockMvc.perform(get("/api/products/" + product.getId()).header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(product.getId()))
				.andExpect(jsonPath("$.name").value("Laptop")).andExpect(jsonPath("$.active").value(true));
	}

	@Test
	void getProductById_shouldReturnProductForAdmin() throws Exception {

		Product product = createProduct("Laptop", "Business laptop", 999.99, true);

		mockMvc.perform(get("/api/products/" + product.getId()).header("X-User-Role", "ADMIN"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(product.getId()))
				.andExpect(jsonPath("$.name").value("Laptop"));
	}

	@Test
	void getProductById_shouldReturnNotFound() throws Exception {

		mockMvc.perform(get("/api/products/99999").header("X-User-Role", "CUSTOMER")).andExpect(status().isNotFound());
	}

	@Test
	void getAllProducts_shouldReturnActiveProductsForCustomer() throws Exception {

		createProduct("Laptop", "Business laptop", 999.99, true);

		createProduct("Phone", "Smartphone", 699.99, true);

		createProduct("Old Tablet", "Inactive tablet", 299.99, false);

		mockMvc.perform(get("/api/products").header("X-User-Role", "CUSTOMER")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void getAllProducts_shouldReturnActiveProductsForAdmin() throws Exception {

		createProduct("Laptop", "Business laptop", 999.99, true);

		createProduct("Phone", "Smartphone", 699.99, true);

		mockMvc.perform(get("/api/products").header("X-User-Role", "ADMIN")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void getAllProductsForAdmin_shouldReturnAllProductsForAdmin() throws Exception {

		createProduct("Laptop", "Business laptop", 999.99, true);

		createProduct("Old Tablet", "Inactive tablet", 299.99, false);

		mockMvc.perform(get("/api/products/admin").header("X-User-Role", "ADMIN")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void getAllProductsForAdmin_shouldRejectCustomer() throws Exception {

		mockMvc.perform(get("/api/products/admin").header("X-User-Role", "CUSTOMER")).andExpect(status().isForbidden());
	}

	@Test
	void updateProduct_shouldUpdateProductForAdmin() throws Exception {

		Product product = createProduct("Laptop", "Business laptop", 999.99, true);

		String request = """
				{
				    "name": "Updated Laptop",
				    "description": "Updated business laptop",
				    "price": 1099.99,
				    "category": "ELECTRONICS",
				    "active": true
				}
				""";

		mockMvc.perform(put("/api/products/" + product.getId()).header("X-User-Role", "ADMIN")
				.contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Updated Laptop"))
				.andExpect(jsonPath("$.description").value("Updated business laptop"))
				.andExpect(jsonPath("$.price").value(1099.99)).andExpect(jsonPath("$.active").value(true));
	}

	@Test
	void updateProduct_shouldRejectCustomer() throws Exception {

		Product product = createProduct("Laptop", "Business laptop", 999.99, true);

		String request = """
				{
				    "name": "Updated Laptop",
				    "description": "Updated laptop",
				    "price": 1099.99,
				    "category": "ELECTRONICS",
				    "active": true
				}
				""";

		mockMvc.perform(put("/api/products/" + product.getId()).header("X-User-Role", "CUSTOMER")
				.contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isForbidden());
	}

	@Test
	void updateProduct_shouldReturnNotFound() throws Exception {

		String request = """
				{
				    "name": "Updated Laptop",
				    "description": "Updated laptop",
				    "price": 1099.99,
				    "category": "ELECTRONICS",
				    "active": true
				}
				""";

		mockMvc.perform(put("/api/products/99999").header("X-User-Role", "ADMIN")
				.contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isNotFound());
	}

	@Test
	void deleteProduct_shouldDeactivateProductForAdmin() throws Exception {

		Product product = createProduct("Laptop", "Business laptop", 999.99, true);

		mockMvc.perform(delete("/api/products/" + product.getId()).header("X-User-Role", "ADMIN"))
				.andExpect(status().isNoContent());

		Product deletedProduct = productRepository.findById(product.getId()).orElseThrow();

		org.junit.jupiter.api.Assertions.assertFalse(deletedProduct.isActive());
	}

	@Test
	void deleteProduct_shouldRejectCustomer() throws Exception {

		Product product = createProduct("Laptop", "Business laptop", 999.99, true);

		mockMvc.perform(delete("/api/products/" + product.getId()).header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isForbidden());
	}

	@Test
	void deleteProduct_shouldReturnNotFound() throws Exception {

		mockMvc.perform(delete("/api/products/99999").header("X-User-Role", "ADMIN")).andExpect(status().isNotFound());
	}

	// =========================================================
	// INTERNAL
	// =========================================================

	@Test
	void getProductForInternalUse_shouldReturnInactiveProduct() throws Exception {

		Product product = createProduct("Old Laptop", "Inactive laptop", 499.99, false);

		mockMvc.perform(get("/api/products/internal/" + product.getId()).header("X-User-Role", "ADMIN"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(product.getId()))
				.andExpect(jsonPath("$.name").value("Old Laptop")).andExpect(jsonPath("$.active").value(false));
	}

	private Product createProduct(String name, String description, double price, boolean active) {

		Product product = Product.builder().name(name).description(description)
				.price(java.math.BigDecimal.valueOf(price)).category(Category.ELECTRONICS).active(active).build();

		return productRepository.save(product);
	}
}