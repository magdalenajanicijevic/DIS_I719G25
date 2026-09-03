package com.ecommerce.orderservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.orderservice.client.InventoryClientResponse;
import com.ecommerce.orderservice.client.InventoryServiceClient;
import com.ecommerce.orderservice.client.OrderClientResponse;
import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.client.ProductClientResponse;
import com.ecommerce.orderservice.client.UserClient;
import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.dto.InventoryQuantityRequest;
import com.ecommerce.orderservice.dto.OrderItemRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.exception.ForbiddenException;
import com.ecommerce.orderservice.exception.InactiveProductException;
import com.ecommerce.orderservice.exception.InsufficientStockException;
import com.ecommerce.orderservice.exception.InvalidOrderStateException;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private ProductClient productClient;

	@Mock
	private InventoryServiceClient inventoryServiceClient;

	@Mock
	private UserClient userClient;

	@InjectMocks
	private OrderServiceImpl orderService;

	private ProductClientResponse product;
	private ProductClientResponse secondProduct;
	private InventoryClientResponse inventory;
	private InventoryClientResponse secondInventory;

	private CreateOrderRequest createOrderRequest;

	private Order order;
	private OrderItem orderItem;

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	@BeforeEach
	void setUp() {

		createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
		updatedAt = LocalDateTime.of(2026, 1, 1, 11, 0);

		product = new ProductClientResponse();
		product.setId(100L);
		product.setName("Laptop");
		product.setPrice(new BigDecimal("1200.00"));
		product.setActive(true);

		secondProduct = new ProductClientResponse();
		secondProduct.setId(200L);
		secondProduct.setName("Mouse");
		secondProduct.setPrice(new BigDecimal("50.00"));
		secondProduct.setActive(true);

		inventory = new InventoryClientResponse();
		inventory.setId(1L);
		inventory.setProductId(100L);
		inventory.setQuantity(20);
		inventory.setReservedQuantity(5);
		inventory.setAvailableQuantity(15);

		secondInventory = new InventoryClientResponse();
		secondInventory.setId(2L);
		secondInventory.setProductId(200L);
		secondInventory.setQuantity(30);
		secondInventory.setReservedQuantity(5);
		secondInventory.setAvailableQuantity(25);

		OrderItemRequest itemRequest = OrderItemRequest.builder().productId(100L).quantity(2).build();

		createOrderRequest = new CreateOrderRequest(10L, List.of(itemRequest));

		orderItem = OrderItem.builder().id(1L).productId(100L).quantity(2).unitPrice(new BigDecimal("1200.00"))
				.subtotal(new BigDecimal("2400.00")).build();

		order = Order.builder().id(1L).userId(10L).totalPrice(new BigDecimal("2400.00")).status(OrderStatus.CREATED)
				.items(List.of(orderItem)).createdAt(createdAt).updatedAt(updatedAt).build();

		orderItem.setOrder(order);
	}

	// =========================================================
	// CREATE ORDER
	// =========================================================

	@Test
	void createOrder_shouldCreateOrderSuccessfully() {

		when(userClient.getUserById(10L)).thenReturn(null);
		when(productClient.getProductById(100L)).thenReturn(product);
		when(inventoryServiceClient.getInventoryByProductId(100L)).thenReturn(inventory);
		when(orderRepository.save(any(Order.class))).thenReturn(order);

		OrderResponse response = orderService.createOrder(createOrderRequest, 10L, "CUSTOMER");

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals(10L, response.getUserId());
		assertEquals(new BigDecimal("2400.00"), response.getTotalPrice());
		assertEquals(OrderStatus.CREATED, response.getStatus());
		assertEquals(1, response.getItems().size());

		verify(userClient).getUserById(10L);
		verify(productClient).getProductById(100L);
		verify(inventoryServiceClient).getInventoryByProductId(100L);
		verify(inventoryServiceClient).reserveInventory(any(InventoryQuantityRequest.class));
		verify(orderRepository).save(any(Order.class));
	}

	@Test
	void createOrder_shouldAllowAdminToCreateOrderForAnotherUser() {

		when(userClient.getUserById(10L)).thenReturn(null);
		when(productClient.getProductById(100L)).thenReturn(product);
		when(inventoryServiceClient.getInventoryByProductId(100L)).thenReturn(inventory);
		when(orderRepository.save(any(Order.class))).thenReturn(order);

		OrderResponse response = orderService.createOrder(createOrderRequest, 999L, "ADMIN");

		assertNotNull(response);
		assertEquals(10L, response.getUserId());

		verify(userClient).getUserById(10L);
		verify(productClient).getProductById(100L);
		verify(inventoryServiceClient).reserveInventory(any(InventoryQuantityRequest.class));
		verify(orderRepository).save(any(Order.class));
	}

	@Test
	void createOrder_shouldRejectCustomerCreatingOrderForAnotherUser() {

		createOrderRequest.setUserId(20L);

		ForbiddenException exception = assertThrows(ForbiddenException.class,
				() -> orderService.createOrder(createOrderRequest, 10L, "CUSTOMER"));

		assertEquals("You can only create orders for yourself.", exception.getMessage());

		verifyNoInteractions(userClient, productClient, inventoryServiceClient, orderRepository);
	}

	@Test
	void createOrder_shouldBuildCorrectOrderWithMultipleItems() {

		OrderItemRequest firstItem = OrderItemRequest.builder().productId(100L).quantity(2).build();

		OrderItemRequest secondItem = OrderItemRequest.builder().productId(200L).quantity(3).build();

		CreateOrderRequest request = new CreateOrderRequest(10L, List.of(firstItem, secondItem));

		Order savedOrder = Order.builder().id(1L).userId(10L).totalPrice(new BigDecimal("2550.00"))
				.status(OrderStatus.CREATED).createdAt(createdAt).updatedAt(updatedAt).build();

		when(userClient.getUserById(10L)).thenReturn(null);
		when(productClient.getProductById(100L)).thenReturn(product);
		when(productClient.getProductById(200L)).thenReturn(secondProduct);
		when(inventoryServiceClient.getInventoryByProductId(100L)).thenReturn(inventory);
		when(inventoryServiceClient.getInventoryByProductId(200L)).thenReturn(secondInventory);
		when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

		OrderResponse response = orderService.createOrder(request, 10L, "CUSTOMER");

		assertNotNull(response);
		assertEquals(new BigDecimal("2550.00"), response.getTotalPrice());

		ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

		verify(orderRepository).save(orderCaptor.capture());

		Order saved = orderCaptor.getValue();

		assertEquals(10L, saved.getUserId());
		assertEquals(new BigDecimal("2550.00"), saved.getTotalPrice());
		assertEquals(2, saved.getItems().size());

		assertEquals(100L, saved.getItems().get(0).getProductId());
		assertEquals(2, saved.getItems().get(0).getQuantity());
		assertEquals(new BigDecimal("1200.00"), saved.getItems().get(0).getUnitPrice());
		assertEquals(new BigDecimal("2400.00"), saved.getItems().get(0).getSubtotal());

		assertEquals(200L, saved.getItems().get(1).getProductId());
		assertEquals(3, saved.getItems().get(1).getQuantity());
		assertEquals(new BigDecimal("50.00"), saved.getItems().get(1).getUnitPrice());
		assertEquals(new BigDecimal("150.00"), saved.getItems().get(1).getSubtotal());
	}

	@Test
	void createOrder_shouldThrowException_whenProductIsInactive() {

		product.setActive(false);

		when(userClient.getUserById(10L)).thenReturn(null);
		when(productClient.getProductById(100L)).thenReturn(product);

		InactiveProductException exception = assertThrows(InactiveProductException.class,
				() -> orderService.createOrder(createOrderRequest, 10L, "CUSTOMER"));

		assertEquals("The selected product is currently unavailable for ordering.", exception.getMessage());

		verify(userClient).getUserById(10L);
		verify(productClient).getProductById(100L);
		verifyNoInteractions(orderRepository, inventoryServiceClient);
	}

	@Test
	void createOrder_shouldThrowException_whenInventoryIsInsufficient() {

		inventory.setAvailableQuantity(1);

		when(userClient.getUserById(10L)).thenReturn(null);
		when(productClient.getProductById(100L)).thenReturn(product);
		when(inventoryServiceClient.getInventoryByProductId(100L)).thenReturn(inventory);

		InsufficientStockException exception = assertThrows(InsufficientStockException.class,
				() -> orderService.createOrder(createOrderRequest, 10L, "CUSTOMER"));

		assertEquals("Insufficient stock available for product with id: 100", exception.getMessage());

		verify(inventoryServiceClient, never()).reserveInventory(any());

		verify(orderRepository, never()).save(any(Order.class));
	}

	@Test
	void createOrder_shouldReleasePreviouslyReservedItems_whenReservationFails() {

		OrderItemRequest firstItem = OrderItemRequest.builder().productId(100L).quantity(2).build();

		OrderItemRequest secondItem = OrderItemRequest.builder().productId(200L).quantity(3).build();

		CreateOrderRequest request = new CreateOrderRequest(10L, List.of(firstItem, secondItem));

		when(userClient.getUserById(10L)).thenReturn(null);
		when(productClient.getProductById(100L)).thenReturn(product);
		when(productClient.getProductById(200L)).thenReturn(secondProduct);
		when(inventoryServiceClient.getInventoryByProductId(100L)).thenReturn(inventory);
		when(inventoryServiceClient.getInventoryByProductId(200L)).thenReturn(secondInventory);

		RuntimeException reservationException = new RuntimeException("Inventory reservation failed.");

		org.mockito.Mockito.doNothing().doThrow(reservationException).when(inventoryServiceClient)
				.reserveInventory(any(InventoryQuantityRequest.class));

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> orderService.createOrder(request, 10L, "CUSTOMER"));

		assertEquals("Inventory reservation failed.", exception.getMessage());

		ArgumentCaptor<InventoryQuantityRequest> releaseCaptor = ArgumentCaptor
				.forClass(InventoryQuantityRequest.class);

		verify(inventoryServiceClient).releaseInventory(releaseCaptor.capture());

		InventoryQuantityRequest releaseRequest = releaseCaptor.getValue();

		assertEquals(100L, releaseRequest.getProductId());
		assertEquals(2, releaseRequest.getQuantity());

		verify(orderRepository, never()).save(any(Order.class));
	}

	@Test
	void getOrderById_shouldReturnOwnOrderForCustomer() {

		when(orderRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(order));

		OrderResponse response = orderService.getOrderById(1L, 10L, "CUSTOMER");

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals(10L, response.getUserId());
		assertEquals(OrderStatus.CREATED, response.getStatus());

		verify(orderRepository).findByIdAndDeletedAtIsNull(1L);
	}

	@Test
	void getOrderById_shouldAllowAdminToAccessAnyOrder() {

		when(orderRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(order));

		OrderResponse response = orderService.getOrderById(1L, 999L, "ADMIN");

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals(10L, response.getUserId());

		verify(orderRepository).findByIdAndDeletedAtIsNull(1L);
	}

	@Test
	void getOrderById_shouldRejectCustomerAccessingAnotherUsersOrder() {

		when(orderRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(order));

		ForbiddenException exception = assertThrows(ForbiddenException.class,
				() -> orderService.getOrderById(1L, 20L, "CUSTOMER"));

		assertEquals("You can only access your own orders.", exception.getMessage());
	}

	@Test
	void getOrderById_shouldThrowException_whenOrderNotFound() {

		when(orderRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

		OrderNotFoundException exception = assertThrows(OrderNotFoundException.class,
				() -> orderService.getOrderById(1L, 10L, "CUSTOMER"));

		assertEquals("Order not found with id: 1", exception.getMessage());
	}

	@Test
	void getAllOrders_shouldReturnAllOrdersForAdmin() {

		when(orderRepository.findByDeletedAtIsNull()).thenReturn(List.of(order));

		List<OrderResponse> response = orderService.getAllOrders("ADMIN");

		assertNotNull(response);
		assertEquals(1, response.size());
		assertEquals(1L, response.get(0).getId());

		verify(orderRepository).findByDeletedAtIsNull();
	}

	@Test
	void getAllOrders_shouldReturnEmptyListForAdmin_whenNoOrdersExist() {

		when(orderRepository.findByDeletedAtIsNull()).thenReturn(List.of());

		List<OrderResponse> response = orderService.getAllOrders("ADMIN");

		assertNotNull(response);
		assertEquals(0, response.size());
	}

	@Test
	void getAllOrders_shouldRejectCustomer() {

		ForbiddenException exception = assertThrows(ForbiddenException.class,
				() -> orderService.getAllOrders("CUSTOMER"));

		assertEquals("Only administrators can access all orders.", exception.getMessage());

		verifyNoInteractions(orderRepository);
	}

	@Test
	void getOrdersByUserId_shouldReturnOwnOrdersForCustomer() {

		when(orderRepository.findByUserIdAndDeletedAtIsNull(10L)).thenReturn(List.of(order));

		List<OrderResponse> response = orderService.getOrdersByUserId(10L, 10L, "CUSTOMER");

		assertNotNull(response);
		assertEquals(1, response.size());
		assertEquals(10L, response.get(0).getUserId());

		verify(orderRepository).findByUserIdAndDeletedAtIsNull(10L);
	}

	@Test
	void getOrdersByUserId_shouldAllowAdminToAccessAnyUserOrders() {

		when(orderRepository.findByUserIdAndDeletedAtIsNull(10L)).thenReturn(List.of(order));

		List<OrderResponse> response = orderService.getOrdersByUserId(10L, 999L, "ADMIN");

		assertNotNull(response);
		assertEquals(1, response.size());
		assertEquals(10L, response.get(0).getUserId());

		verify(orderRepository).findByUserIdAndDeletedAtIsNull(10L);
	}

	@Test
	void getOrdersByUserId_shouldRejectCustomerAccessingAnotherUsersOrders() {

		ForbiddenException exception = assertThrows(ForbiddenException.class,
				() -> orderService.getOrdersByUserId(20L, 10L, "CUSTOMER"));

		assertEquals("You can only access your own orders.", exception.getMessage());

		verifyNoInteractions(orderRepository);
	}

	@Test
	void getOrdersByUserId_shouldReturnEmptyList_whenUserHasNoOrders() {

		when(orderRepository.findByUserIdAndDeletedAtIsNull(10L)).thenReturn(List.of());

		List<OrderResponse> response = orderService.getOrdersByUserId(10L, 10L, "CUSTOMER");

		assertNotNull(response);
		assertEquals(0, response.size());
	}

	@Test
	void deleteOrder_shouldSoftDeleteOrderForAdmin() {

		when(orderRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(order));

		orderService.deleteOrder(1L, "ADMIN");

		assertNotNull(order.getDeletedAt());

		verify(orderRepository).save(order);
	}

	@Test
	void deleteOrder_shouldRejectCustomer() {

		ForbiddenException exception = assertThrows(ForbiddenException.class,
				() -> orderService.deleteOrder(1L, "CUSTOMER"));

		assertEquals("Only administrators can delete orders.", exception.getMessage());

		verifyNoInteractions(orderRepository);
	}

	@Test
	void deleteOrder_shouldThrowException_whenOrderNotFound() {

		when(orderRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

		assertThrows(OrderNotFoundException.class, () -> orderService.deleteOrder(1L, "ADMIN"));

		verify(orderRepository, never()).save(any(Order.class));
	}

	@Test
	void confirmPayment_shouldDeductInventoryAndMarkOrderAsPaid() {

		when(orderRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(order));

		orderService.confirmPayment(1L);

		assertEquals(OrderStatus.PAID, order.getStatus());

		verify(inventoryServiceClient).deductInventory(any(InventoryQuantityRequest.class));

		verify(orderRepository).save(order);
	}

	@Test
	void confirmPayment_shouldPassCorrectQuantityToInventory() {

		when(orderRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(order));

		ArgumentCaptor<InventoryQuantityRequest> captor = ArgumentCaptor.forClass(InventoryQuantityRequest.class);

		orderService.confirmPayment(1L);

		verify(inventoryServiceClient).deductInventory(captor.capture());

		InventoryQuantityRequest request = captor.getValue();

		assertEquals(100L, request.getProductId());
		assertEquals(2, request.getQuantity());
	}

	@Test
	void confirmPayment_shouldThrowException_whenOrderIsAlreadyPaid() {

		order.setStatus(OrderStatus.PAID);

		when(orderRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(order));

		InvalidOrderStateException exception = assertThrows(InvalidOrderStateException.class,
				() -> orderService.confirmPayment(1L));

		assertEquals("Only orders in CREATED status can be paid.", exception.getMessage());

		verify(inventoryServiceClient, never()).deductInventory(any());

		verify(orderRepository, never()).save(any(Order.class));
	}

	@Test
	void confirmPayment_shouldThrowException_whenOrderIsCancelled() {

		order.setStatus(OrderStatus.CANCELLED);

		when(orderRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(order));

		InvalidOrderStateException exception = assertThrows(InvalidOrderStateException.class,
				() -> orderService.confirmPayment(1L));

		assertEquals("Only orders in CREATED status can be paid.", exception.getMessage());

		verify(inventoryServiceClient, never()).deductInventory(any());

		verify(orderRepository, never()).save(any(Order.class));
	}

	@Test
	void cancelOrder_shouldReleaseInventoryAndMarkOrderAsCancelled() {

		when(orderRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(order));

		when(orderRepository.save(order)).thenReturn(order);

		OrderResponse response = orderService.cancelOrder(1L, 10L, "CUSTOMER");

		assertEquals(OrderStatus.CANCELLED, order.getStatus());

		assertNotNull(response);
		assertEquals(OrderStatus.CANCELLED, response.getStatus());

		verify(inventoryServiceClient).releaseInventory(any(InventoryQuantityRequest.class));

		verify(orderRepository).save(order);
	}

	@Test
	void cancelOrder_shouldAllowAdminToCancelAnyOrder() {

		when(orderRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(order));

		when(orderRepository.save(order)).thenReturn(order);

		OrderResponse response = orderService.cancelOrder(1L, 999L, "ADMIN");

		assertNotNull(response);
		assertEquals(OrderStatus.CANCELLED, response.getStatus());

		verify(inventoryServiceClient).releaseInventory(any(InventoryQuantityRequest.class));

		verify(orderRepository).save(order);
	}

	@Test
	void cancelOrder_shouldRejectCustomerCancellingAnotherUsersOrder() {

		when(orderRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(order));

		ForbiddenException exception = assertThrows(ForbiddenException.class,
				() -> orderService.cancelOrder(1L, 20L, "CUSTOMER"));

		assertEquals("You can only cancel your own orders.", exception.getMessage());

		verify(inventoryServiceClient, never()).releaseInventory(any());

		verify(orderRepository, never()).save(any(Order.class));
	}

	@Test
	void cancelOrder_shouldPassCorrectQuantityToInventory() {

		when(orderRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(order));

		when(orderRepository.save(order)).thenReturn(order);

		ArgumentCaptor<InventoryQuantityRequest> captor = ArgumentCaptor.forClass(InventoryQuantityRequest.class);

		orderService.cancelOrder(1L, 10L, "CUSTOMER");

		verify(inventoryServiceClient).releaseInventory(captor.capture());

		InventoryQuantityRequest request = captor.getValue();

		assertEquals(100L, request.getProductId());
		assertEquals(2, request.getQuantity());
	}

	@Test
	void cancelOrder_shouldThrowException_whenOrderIsAlreadyPaid() {

		order.setStatus(OrderStatus.PAID);

		when(orderRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(order));

		InvalidOrderStateException exception = assertThrows(InvalidOrderStateException.class,
				() -> orderService.cancelOrder(1L, 10L, "CUSTOMER"));

		assertEquals("Only orders in CREATED status can be cancelled.", exception.getMessage());

		verify(inventoryServiceClient, never()).releaseInventory(any());

		verify(orderRepository, never()).save(any(Order.class));
	}

	@Test
	void cancelOrder_shouldThrowException_whenOrderIsCancelled() {

		order.setStatus(OrderStatus.CANCELLED);

		when(orderRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(order));

		InvalidOrderStateException exception = assertThrows(InvalidOrderStateException.class,
				() -> orderService.cancelOrder(1L, 10L, "CUSTOMER"));

		assertEquals("Only orders in CREATED status can be cancelled.", exception.getMessage());

		verify(inventoryServiceClient, never()).releaseInventory(any());

		verify(orderRepository, never()).save(any(Order.class));
	}

	@Test
	void cancelOrder_shouldThrowException_whenOrderNotFound() {

		when(orderRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

		assertThrows(OrderNotFoundException.class, () -> orderService.cancelOrder(1L, 10L, "CUSTOMER"));

		verify(inventoryServiceClient, never()).releaseInventory(any());

		verify(orderRepository, never()).save(any(Order.class));
	}

	// =========================================================
	// INTERNAL ORDER
	// =========================================================

	@Test
	void getInternalOrder_shouldReturnCorrectClientResponse() {

		when(orderRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(order));

		OrderClientResponse response = orderService.getInternalOrder(1L);

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals(10L, response.getUserId());
		assertEquals(new BigDecimal("2400.00"), response.getTotalPrice());
		assertEquals("CREATED", response.getStatus());

		assertEquals(1, response.getItems().size());

		assertEquals(100L, response.getItems().get(0).getProductId());

		assertEquals(2, response.getItems().get(0).getQuantity());

		assertEquals(new BigDecimal("1200.00"), response.getItems().get(0).getUnitPrice());

		assertEquals(new BigDecimal("2400.00"), response.getItems().get(0).getSubtotal());
	}

	@Test
	void getInternalOrder_shouldThrowException_whenOrderNotFound() {

		when(orderRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

		assertThrows(OrderNotFoundException.class, () -> orderService.getInternalOrder(1L));
	}

	// =========================================================
	// HAS PURCHASED PRODUCT
	// =========================================================

	@Test
	void hasPurchasedProduct_shouldReturnTrue_whenPaidOrderContainsProduct() {

		order.setStatus(OrderStatus.PAID);

		when(orderRepository.findByUserIdAndDeletedAtIsNull(10L)).thenReturn(List.of(order));

		boolean result = orderService.hasPurchasedProduct(10L, 100L);

		assertTrue(result);

		verify(orderRepository).findByUserIdAndDeletedAtIsNull(10L);
	}

	@Test
	void hasPurchasedProduct_shouldReturnFalse_whenPaidOrderDoesNotContainProduct() {

		order.setStatus(OrderStatus.PAID);

		when(orderRepository.findByUserIdAndDeletedAtIsNull(10L)).thenReturn(List.of(order));

		boolean result = orderService.hasPurchasedProduct(10L, 999L);

		assertFalse(result);
	}

	@Test
	void hasPurchasedProduct_shouldReturnFalse_whenOrderIsNotPaid() {

		order.setStatus(OrderStatus.CREATED);

		when(orderRepository.findByUserIdAndDeletedAtIsNull(10L)).thenReturn(List.of(order));

		boolean result = orderService.hasPurchasedProduct(10L, 100L);

		assertFalse(result);
	}

	@Test
	void hasPurchasedProduct_shouldReturnFalse_whenUserHasNoOrders() {

		when(orderRepository.findByUserIdAndDeletedAtIsNull(10L)).thenReturn(List.of());

		boolean result = orderService.hasPurchasedProduct(10L, 100L);

		assertFalse(result);
	}

	@Test
	void hasPurchasedProduct_shouldReturnTrue_whenProductExistsInOneOfMultiplePaidOrders() {

		Order firstOrder = Order.builder().id(1L).userId(10L).totalPrice(new BigDecimal("50.00"))
				.status(OrderStatus.PAID).items(List.of(OrderItem.builder().productId(200L).quantity(1)
						.unitPrice(new BigDecimal("50.00")).subtotal(new BigDecimal("50.00")).build()))
				.build();

		Order secondOrder = Order.builder().id(2L).userId(10L).totalPrice(new BigDecimal("2400.00"))
				.status(OrderStatus.PAID).items(List.of(orderItem)).build();

		when(orderRepository.findByUserIdAndDeletedAtIsNull(10L)).thenReturn(List.of(firstOrder, secondOrder));

		boolean result = orderService.hasPurchasedProduct(10L, 100L);

		assertTrue(result);
	}

	@Test
	void hasPurchasedProduct_shouldIgnoreCancelledOrders() {

		order.setStatus(OrderStatus.CANCELLED);

		when(orderRepository.findByUserIdAndDeletedAtIsNull(10L)).thenReturn(List.of(order));

		boolean result = orderService.hasPurchasedProduct(10L, 100L);

		assertFalse(result);
	}
}