package com.ecommerce.inventoryservice.integration;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ecommerce.inventoryservice.client.ProductClient;
import com.ecommerce.inventoryservice.client.ProductClientResponse;
import com.ecommerce.inventoryservice.entity.Inventory;
import com.ecommerce.inventoryservice.repository.InventoryRepository;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class InventoryServiceIntegrationTest {

	@SuppressWarnings("resource")
	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18").withDatabaseName("inventory_db")
			.withUsername("postgres").withPassword("postgres");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private InventoryRepository inventoryRepository;

	@MockitoBean
	private ProductClient productClient;

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

		inventoryRepository.deleteAll();
	}

	@Test
	void createInventory_shouldCreateInventoryForAdmin() throws Exception {

		when(productClient.getProductById(100L)).thenReturn(new ProductClientResponse(100L, "Laptop", true));

		String request = """
				{
				    "productId": 100,
				    "quantity": 50
				}
				""";

		mockMvc.perform(post("/api/inventory").header("X-User-Role", "ADMIN").contentType(MediaType.APPLICATION_JSON)
				.content(request)).andExpect(status().isCreated()).andExpect(jsonPath("$.id", notNullValue()))
				.andExpect(jsonPath("$.productId").value(100)).andExpect(jsonPath("$.quantity").value(50))
				.andExpect(jsonPath("$.reservedQuantity").value(0))
				.andExpect(jsonPath("$.availableQuantity").value(50));
	}

	@Test
	void createInventory_shouldRejectCustomer() throws Exception {

		String request = """
				{
				    "productId": 100,
				    "quantity": 50
				}
				""";

		mockMvc.perform(post("/api/inventory").header("X-User-Role", "CUSTOMER").contentType(MediaType.APPLICATION_JSON)
				.content(request)).andExpect(status().isForbidden());
	}

	@Test
	void createInventory_shouldRejectInactiveProduct() throws Exception {

		when(productClient.getProductById(100L)).thenReturn(new ProductClientResponse(100L, "Laptop", false));

		String request = """
				{
				    "productId": 100,
				    "quantity": 50
				}
				""";

		mockMvc.perform(post("/api/inventory").header("X-User-Role", "ADMIN").contentType(MediaType.APPLICATION_JSON)
				.content(request)).andExpect(status().isBadRequest());
	}

	@Test
	void createInventory_shouldRejectDuplicateProduct() throws Exception {

		when(productClient.getProductById(100L)).thenReturn(new ProductClientResponse(100L, "Laptop", true));

		Inventory existing = Inventory.builder().productId(100L).quantity(50).reservedQuantity(0).build();

		inventoryRepository.save(existing);

		String request = """
				{
				    "productId": 100,
				    "quantity": 30
				}
				""";

		mockMvc.perform(post("/api/inventory").header("X-User-Role", "ADMIN").contentType(MediaType.APPLICATION_JSON)
				.content(request)).andExpect(status().isConflict());
	}

	@Test
	void getInventoryById_shouldReturnInventoryForAdmin() throws Exception {

		Inventory inventory = Inventory.builder().productId(100L).quantity(50).reservedQuantity(10).build();

		Inventory saved = inventoryRepository.save(inventory);

		mockMvc.perform(get("/api/inventory/" + saved.getId()).header("X-User-Role", "ADMIN"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(saved.getId()))
				.andExpect(jsonPath("$.productId").value(100)).andExpect(jsonPath("$.quantity").value(50))
				.andExpect(jsonPath("$.reservedQuantity").value(10))
				.andExpect(jsonPath("$.availableQuantity").value(40));
	}

	@Test
	void getInventoryById_shouldRejectCustomer() throws Exception {

		Inventory inventory = Inventory.builder().productId(100L).quantity(50).reservedQuantity(10).build();

		Inventory saved = inventoryRepository.save(inventory);

		mockMvc.perform(get("/api/inventory/" + saved.getId()).header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isForbidden());
	}

	@Test
	void getInventoryById_shouldReturnNotFound() throws Exception {

		mockMvc.perform(get("/api/inventory/99999").header("X-User-Role", "ADMIN")).andExpect(status().isNotFound());
	}

	@Test
	void getInventoryByProductId_shouldReturnInventoryForAdmin() throws Exception {

		Inventory inventory = Inventory.builder().productId(100L).quantity(50).reservedQuantity(10).build();

		inventoryRepository.save(inventory);

		mockMvc.perform(get("/api/inventory/product/100").header("X-User-Role", "ADMIN")).andExpect(status().isOk())
				.andExpect(jsonPath("$.productId").value(100)).andExpect(jsonPath("$.quantity").value(50))
				.andExpect(jsonPath("$.availableQuantity").value(40));
	}

	@Test
	void getInventoryByProductId_shouldRejectCustomer() throws Exception {

		mockMvc.perform(get("/api/inventory/product/100").header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isForbidden());
	}

	@Test
	void getInventoryByProductId_shouldReturnNotFound() throws Exception {

		mockMvc.perform(get("/api/inventory/product/999").header("X-User-Role", "ADMIN"))
				.andExpect(status().isNotFound());
	}

	@Test
	void getAllInventory_shouldReturnAllInventoryForAdmin() throws Exception {

		inventoryRepository.save(Inventory.builder().productId(100L).quantity(50).reservedQuantity(10).build());

		inventoryRepository.save(Inventory.builder().productId(200L).quantity(30).reservedQuantity(5).build());

		mockMvc.perform(get("/api/inventory").header("X-User-Role", "ADMIN")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void getAllInventory_shouldRejectCustomer() throws Exception {

		mockMvc.perform(get("/api/inventory").header("X-User-Role", "CUSTOMER")).andExpect(status().isForbidden());
	}

	@Test
	void getAllInventory_shouldReturnEmptyList() throws Exception {

		mockMvc.perform(get("/api/inventory").header("X-User-Role", "ADMIN")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void getInventoryByProductIdInternal_shouldReturnInventory() throws Exception {

		Inventory inventory = Inventory.builder().productId(100L).quantity(50).reservedQuantity(10).build();

		inventoryRepository.save(inventory);

		mockMvc.perform(get("/api/inventory/internal/product/100")).andExpect(status().isOk())
				.andExpect(jsonPath("$.productId").value(100)).andExpect(jsonPath("$.quantity").value(50))
				.andExpect(jsonPath("$.reservedQuantity").value(10));
	}

	@Test
	void updateInventory_shouldUpdateInventoryForAdmin() throws Exception {

		Inventory inventory = Inventory.builder().productId(100L).quantity(50).reservedQuantity(10).build();

		Inventory saved = inventoryRepository.save(inventory);

		String request = """
				{
				    "quantity": 80
				}
				""";

		mockMvc.perform(put("/api/inventory/" + saved.getId()).header("X-User-Role", "ADMIN")
				.contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isOk())
				.andExpect(jsonPath("$.quantity").value(80)).andExpect(jsonPath("$.reservedQuantity").value(10))
				.andExpect(jsonPath("$.availableQuantity").value(70));
	}

	@Test
	void updateInventory_shouldRejectCustomer() throws Exception {

		Inventory inventory = Inventory.builder().productId(100L).quantity(50).reservedQuantity(10).build();

		Inventory saved = inventoryRepository.save(inventory);

		String request = """
				{
				    "quantity": 80
				}
				""";

		mockMvc.perform(put("/api/inventory/" + saved.getId()).header("X-User-Role", "CUSTOMER")
				.contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isForbidden());
	}

	@Test
	void updateInventory_shouldReturnNotFound() throws Exception {

		String request = """
				{
				    "quantity": 80
				}
				""";

		mockMvc.perform(put("/api/inventory/99999").header("X-User-Role", "ADMIN")
				.contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isNotFound());
	}

	@Test
	void deleteInventory_shouldDeleteInventoryForAdmin() throws Exception {

		Inventory inventory = Inventory.builder().productId(100L).quantity(50).reservedQuantity(10).build();

		Inventory saved = inventoryRepository.save(inventory);

		mockMvc.perform(delete("/api/inventory/" + saved.getId()).header("X-User-Role", "ADMIN"))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/inventory/" + saved.getId()).header("X-User-Role", "ADMIN"))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteInventory_shouldRejectCustomer() throws Exception {

		Inventory inventory = Inventory.builder().productId(100L).quantity(50).reservedQuantity(10).build();

		Inventory saved = inventoryRepository.save(inventory);

		mockMvc.perform(delete("/api/inventory/" + saved.getId()).header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isForbidden());
	}
	
	@Test
	void reserveInventory_shouldReserveStock() throws Exception {

		Inventory inventory = Inventory.builder().productId(100L).quantity(50).reservedQuantity(10).build();

		inventoryRepository.save(inventory);

		String request = """
				{
				    "productId": 100,
				    "quantity": 10
				}
				""";

		mockMvc.perform(
				post("/api/inventory/internal/reserve").contentType(MediaType.APPLICATION_JSON).content(request))
				.andExpect(status().isOk());

		Inventory updated = inventoryRepository.findByProductId(100L).orElseThrow();

		org.junit.jupiter.api.Assertions.assertEquals(20, updated.getReservedQuantity());
	}

	@Test
	void reserveInventory_shouldRejectInsufficientStock() throws Exception {

		Inventory inventory = Inventory.builder().productId(100L).quantity(50).reservedQuantity(40).build();

		inventoryRepository.save(inventory);

		String request = """
				{
				    "productId": 100,
				    "quantity": 11
				}
				""";

		mockMvc.perform(
				post("/api/inventory/internal/reserve").contentType(MediaType.APPLICATION_JSON).content(request))
				.andExpect(status().isBadRequest());
	}

	@Test
	void releaseInventory_shouldReleaseReservedStock() throws Exception {

		Inventory inventory = Inventory.builder().productId(100L).quantity(50).reservedQuantity(20).build();

		inventoryRepository.save(inventory);

		String request = """
				{
				    "productId": 100,
				    "quantity": 10
				}
				""";

		mockMvc.perform(
				post("/api/inventory/internal/release").contentType(MediaType.APPLICATION_JSON).content(request))
				.andExpect(status().isOk());

		Inventory updated = inventoryRepository.findByProductId(100L).orElseThrow();

		org.junit.jupiter.api.Assertions.assertEquals(10, updated.getReservedQuantity());
	}

	@Test
	void releaseInventory_shouldRejectExcessiveRelease() throws Exception {

		Inventory inventory = Inventory.builder().productId(100L).quantity(50).reservedQuantity(10).build();

		inventoryRepository.save(inventory);

		String request = """
				{
				    "productId": 100,
				    "quantity": 11
				}
				""";

		mockMvc.perform(
				post("/api/inventory/internal/release").contentType(MediaType.APPLICATION_JSON).content(request))
				.andExpect(status().isBadRequest());
	}

	@Test
	void deductInventory_shouldDeductReservedStock() throws Exception {

		Inventory inventory = Inventory.builder().productId(100L).quantity(50).reservedQuantity(20).build();

		inventoryRepository.save(inventory);

		String request = """
				{
				    "productId": 100,
				    "quantity": 10
				}
				""";

		mockMvc.perform(post("/api/inventory/internal/deduct").contentType(MediaType.APPLICATION_JSON).content(request))
				.andExpect(status().isOk());

		Inventory updated = inventoryRepository.findByProductId(100L).orElseThrow();

		org.junit.jupiter.api.Assertions.assertEquals(40, updated.getQuantity());

		org.junit.jupiter.api.Assertions.assertEquals(10, updated.getReservedQuantity());
	}

	@Test
	void deductInventory_shouldRejectExcessiveDeduction() throws Exception {

		Inventory inventory = Inventory.builder().productId(100L).quantity(50).reservedQuantity(10).build();

		inventoryRepository.save(inventory);

		String request = """
				{
				    "productId": 100,
				    "quantity": 11
				}
				""";

		mockMvc.perform(post("/api/inventory/internal/deduct").contentType(MediaType.APPLICATION_JSON).content(request))
				.andExpect(status().isBadRequest());
	}
}