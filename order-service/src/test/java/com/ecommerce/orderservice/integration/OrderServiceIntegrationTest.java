package com.ecommerce.orderservice.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerce.orderservice.client.InventoryClientResponse;
import com.ecommerce.orderservice.client.InventoryServiceClient;
import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.client.ProductClientResponse;
import com.ecommerce.orderservice.client.UserClient;
import com.ecommerce.orderservice.client.UserClientResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.repository.OrderRepository;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class OrderServiceIntegrationTest {

	@SuppressWarnings("resource")
	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18").withDatabaseName("order_db")
			.withUsername("postgres").withPassword("postgres");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrderRepository orderRepository;

	@MockitoBean
	private UserClient userClient;

	@MockitoBean
	private ProductClient productClient;

	@MockitoBean
	private InventoryServiceClient inventoryServiceClient;

	private UserClientResponse user;
	private ProductClientResponse product;
	private InventoryClientResponse inventory;

	@BeforeEach
	void setUp() {

		orderRepository.deleteAll();

		user = new UserClientResponse();
		user.setId(1L);
		user.setFirstName("John");
		user.setLastName("Smith");
		user.setEmail("john.smith@example.com");
		user.setRole("CUSTOMER");

		product = new ProductClientResponse();
		product.setId(100L);
		product.setName("Test Product");
		product.setPrice(new BigDecimal("25.00"));
		product.setActive(true);

		inventory = new InventoryClientResponse();
		inventory.setId(1L);
		inventory.setProductId(100L);
		inventory.setQuantity(50);
		inventory.setReservedQuantity(10);
		inventory.setAvailableQuantity(40);

		when(userClient.getUserById(1L)).thenReturn(user);
		when(productClient.getProductById(100L)).thenReturn(product);
		when(inventoryServiceClient.getInventoryByProductId(100L)).thenReturn(inventory);

		doNothing().when(inventoryServiceClient).reserveInventory(any());
		doNothing().when(inventoryServiceClient).releaseInventory(any());
		doNothing().when(inventoryServiceClient).deductInventory(any());
	}

	@Test
	void createOrder_shouldCreateOrderForCustomer() throws Exception {

		String request = """
				{
					"userId": 1,
					"items": [
						{
							"productId": 100,
							"quantity": 2
						}
					]
				}
				""";

		mockMvc.perform(post("/api/orders").header("X-User-Id", 1L).header("X-User-Role", "CUSTOMER")
				.contentType("application/json").content(request)).andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId").value(1)).andExpect(jsonPath("$.totalPrice").value(50.00))
				.andExpect(jsonPath("$.status").value("CREATED")).andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].productId").value(100))
				.andExpect(jsonPath("$.items[0].quantity").value(2))
				.andExpect(jsonPath("$.items[0].unitPrice").value(25.00))
				.andExpect(jsonPath("$.items[0].subtotal").value(50.00));
	}

	@Test
	void createOrder_shouldCreateOrderForAdmin() throws Exception {

		String request = """
				{
					"userId": 1,
					"items": [
						{
							"productId": 100,
							"quantity": 2
						}
					]
				}
				""";

		mockMvc.perform(post("/api/orders").header("X-User-Id", 1L).header("X-User-Role", "ADMIN")
				.contentType("application/json").content(request)).andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId").value(1)).andExpect(jsonPath("$.totalPrice").value(50.00))
				.andExpect(jsonPath("$.status").value("CREATED"));
	}

	@Test
	void createOrder_shouldRejectInvalidRequest() throws Exception {

		String request = """
				{
					"userId": null,
					"items": []
				}
				""";

		mockMvc.perform(post("/api/orders").header("X-User-Id", 1L).header("X-User-Role", "CUSTOMER")
				.contentType("application/json").content(request)).andExpect(status().isBadRequest());
	}

	@Test
	void createOrder_shouldRejectInactiveProduct() throws Exception {

		product.setActive(false);

		String request = """
				{
					"userId": 1,
					"items": [
						{
							"productId": 100,
							"quantity": 2
						}
					]
				}
				""";

		mockMvc.perform(post("/api/orders").header("X-User-Id", 1L).header("X-User-Role", "CUSTOMER")
				.contentType("application/json").content(request)).andExpect(status().isBadRequest());
	}

	@Test
	void createOrder_shouldRejectInsufficientStock() throws Exception {

		inventory.setAvailableQuantity(1);

		String request = """
				{
					"userId": 1,
					"items": [
						{
							"productId": 100,
							"quantity": 2
						}
					]
				}
				""";

		mockMvc.perform(post("/api/orders").header("X-User-Id", 1L).header("X-User-Role", "CUSTOMER")
				.contentType("application/json").content(request)).andExpect(status().isConflict());
	}

	@Test
	void getOrderById_shouldReturnOwnOrderForCustomer() throws Exception {

		Order order = createOrderWithItem();
		order = orderRepository.save(order);

		mockMvc.perform(get("/api/orders/" + order.getId()).header("X-User-Id", 1L).header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(order.getId()))
				.andExpect(jsonPath("$.userId").value(1)).andExpect(jsonPath("$.totalPrice").value(50.00))
				.andExpect(jsonPath("$.status").value("CREATED")).andExpect(jsonPath("$.items.length()").value(1));
	}

	@Test
	void getOrderById_shouldReturnNotFound() throws Exception {

		mockMvc.perform(get("/api/orders/99999").header("X-User-Id", 1L).header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isNotFound());
	}

	@Test
	void getOrderById_shouldRejectCustomerAccessToAnotherUserOrder() throws Exception {

		Order order = Order.builder().userId(2L).totalPrice(new BigDecimal("50.00")).status(OrderStatus.CREATED)
				.build();

		order = orderRepository.save(order);

		mockMvc.perform(get("/api/orders/" + order.getId()).header("X-User-Id", 1L).header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isForbidden());
	}

	@Test
	void getOrderById_shouldAllowAdminToAccessAnotherUserOrder() throws Exception {

		Order order = Order.builder().userId(2L).totalPrice(new BigDecimal("50.00")).status(OrderStatus.CREATED)
				.build();

		order = orderRepository.save(order);

		mockMvc.perform(get("/api/orders/" + order.getId()).header("X-User-Id", 1L).header("X-User-Role", "ADMIN"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(2));
	}

	@Test
	void getAllOrders_shouldReturnOrdersForAdmin() throws Exception {

		Order order1 = createOrderEntity();

		Order order2 = Order.builder().userId(2L).totalPrice(new BigDecimal("30.00")).status(OrderStatus.CREATED)
				.build();

		orderRepository.saveAll(List.of(order1, order2));

		mockMvc.perform(get("/api/orders").header("X-User-Id", 1L).header("X-User-Role", "ADMIN"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void getAllOrders_shouldRejectCustomer() throws Exception {

		Order order1 = createOrderEntity();

		Order order2 = Order.builder().userId(2L).totalPrice(new BigDecimal("30.00")).status(OrderStatus.CREATED)
				.build();

		orderRepository.saveAll(List.of(order1, order2));

		mockMvc.perform(get("/api/orders").header("X-User-Id", 1L).header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isForbidden());
	}

	@Test
	void getAllOrders_shouldExcludeDeletedOrders() throws Exception {

		Order activeOrder = createOrderEntity();

		Order deletedOrder = Order.builder().userId(2L).totalPrice(new BigDecimal("30.00")).status(OrderStatus.CREATED)
				.deletedAt(LocalDateTime.now()).build();

		orderRepository.save(activeOrder);
		orderRepository.save(deletedOrder);

		mockMvc.perform(get("/api/orders").header("X-User-Id", 1L).header("X-User-Role", "ADMIN"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void getOrdersByUserId_shouldReturnOwnOrdersForCustomer() throws Exception {

		Order order1 = createOrderEntity();

		Order order2 = Order.builder().userId(2L).totalPrice(new BigDecimal("30.00")).status(OrderStatus.CREATED)
				.build();

		orderRepository.save(order1);
		orderRepository.save(order2);

		mockMvc.perform(get("/api/orders/user/1").header("X-User-Id", 1L).header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].userId").value(1));
	}

	@Test
	void getOrdersByUserId_shouldRejectCustomerAccessToAnotherUserOrders() throws Exception {

		mockMvc.perform(get("/api/orders/user/2").header("X-User-Id", 1L).header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isForbidden());
	}

	@Test
	void getOrdersByUserId_shouldAllowAdmin() throws Exception {

		Order order = Order.builder().userId(2L).totalPrice(new BigDecimal("30.00")).status(OrderStatus.CREATED)
				.build();

		orderRepository.save(order);

		mockMvc.perform(get("/api/orders/user/2").header("X-User-Id", 1L).header("X-User-Role", "ADMIN"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void deleteOrder_shouldSoftDeleteOrderForAdmin() throws Exception {

		Order order = createOrderEntity();
		order = orderRepository.save(order);

		mockMvc.perform(delete("/api/orders/" + order.getId()).header("X-User-Role", "ADMIN"))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/orders/" + order.getId()).header("X-User-Id", 1L).header("X-User-Role", "ADMIN"))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteOrder_shouldRejectCustomer() throws Exception {

		Order order = createOrderEntity();
		order = orderRepository.save(order);

		mockMvc.perform(delete("/api/orders/" + order.getId()).header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isForbidden());
	}

	@Test
	void deleteOrder_shouldReturnNotFound() throws Exception {

		mockMvc.perform(delete("/api/orders/99999").header("X-User-Role", "ADMIN")).andExpect(status().isNotFound());
	}

	@Test
	void cancelOrder_shouldCancelOwnCreatedOrder() throws Exception {

		Order order = createOrderWithItem();
		order = orderRepository.save(order);

		mockMvc.perform(post("/api/orders/" + order.getId() + "/cancel").header("X-User-Id", 1L).header("X-User-Role",
				"CUSTOMER")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
	}

	@Test
	void cancelOrder_shouldRejectCustomerAccessToAnotherUserOrder() throws Exception {

		Order order = Order.builder().userId(2L).totalPrice(new BigDecimal("50.00")).status(OrderStatus.CREATED)
				.build();

		order = orderRepository.save(order);

		mockMvc.perform(post("/api/orders/" + order.getId() + "/cancel").header("X-User-Id", 1L).header("X-User-Role",
				"CUSTOMER")).andExpect(status().isForbidden());
	}

	@Test
	void cancelOrder_shouldAllowAdmin() throws Exception {

		Order order = createOrderWithItem();
		order = orderRepository.save(order);

		mockMvc.perform(
				post("/api/orders/" + order.getId() + "/cancel").header("X-User-Id", 1L).header("X-User-Role", "ADMIN"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
	}

	@Test
	void cancelOrder_shouldRejectPaidOrder() throws Exception {

		Order order = Order.builder().userId(1L).totalPrice(new BigDecimal("50.00")).status(OrderStatus.PAID).build();

		order = orderRepository.save(order);

		mockMvc.perform(post("/api/orders/" + order.getId() + "/cancel").header("X-User-Id", 1L).header("X-User-Role",
				"CUSTOMER")).andExpect(status().isBadRequest());
	}

	@Test
	void cancelOrder_shouldReturnNotFound() throws Exception {

		mockMvc.perform(post("/api/orders/99999/cancel").header("X-User-Id", 1L).header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isNotFound());
	}

	@Test
	void getInternalOrder_shouldReturnOrder() throws Exception {

		Order order = createOrderWithItem();
		order = orderRepository.save(order);

		mockMvc.perform(get("/api/orders/internal/" + order.getId())).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(order.getId())).andExpect(jsonPath("$.userId").value(1))
				.andExpect(jsonPath("$.totalPrice").value(50.00)).andExpect(jsonPath("$.status").value("CREATED"))
				.andExpect(jsonPath("$.items.length()").value(1));
	}

	@Test
	void getInternalOrder_shouldReturnNotFound() throws Exception {

		mockMvc.perform(get("/api/orders/internal/99999")).andExpect(status().isNotFound());
	}

	@Test
	void hasPurchasedProduct_shouldReturnTrueForPaidOrder() throws Exception {

		Order order = Order.builder().userId(1L).totalPrice(new BigDecimal("50.00")).status(OrderStatus.PAID).build();

		OrderItem item = OrderItem.builder().productId(100L).quantity(2).unitPrice(new BigDecimal("25.00"))
				.subtotal(new BigDecimal("50.00")).build();

		order.addItem(item);
		orderRepository.save(order);

		mockMvc.perform(get("/api/orders/internal/purchased").param("userId", "1").param("productId", "100"))
				.andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
	}

	@Test
	void hasPurchasedProduct_shouldReturnFalseForCreatedOrder() throws Exception {

		Order order = createOrderWithItem();
		orderRepository.save(order);

		mockMvc.perform(get("/api/orders/internal/purchased").param("userId", "1").param("productId", "100"))
				.andExpect(status().isOk()).andExpect(jsonPath("$").value(false));
	}

	@Test
	void hasPurchasedProduct_shouldReturnFalseForDifferentProduct() throws Exception {

		Order order = Order.builder().userId(1L).totalPrice(new BigDecimal("50.00")).status(OrderStatus.PAID).build();

		OrderItem item = OrderItem.builder().productId(100L).quantity(2).unitPrice(new BigDecimal("25.00"))
				.subtotal(new BigDecimal("50.00")).build();

		order.addItem(item);
		orderRepository.save(order);

		mockMvc.perform(get("/api/orders/internal/purchased").param("userId", "1").param("productId", "200"))
				.andExpect(status().isOk()).andExpect(jsonPath("$").value(false));
	}

	private Order createOrderEntity() {

		return Order.builder().userId(1L).totalPrice(new BigDecimal("50.00")).status(OrderStatus.CREATED).build();
	}

	private Order createOrderWithItem() {

		Order order = createOrderEntity();

		OrderItem item = OrderItem.builder().productId(100L).quantity(2).unitPrice(new BigDecimal("25.00"))
				.subtotal(new BigDecimal("50.00")).build();

		order.addItem(item);

		return order;
	}
}