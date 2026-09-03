package com.ecommerce.orderservice.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ecommerce.orderservice.client.InventoryClientResponse;
import com.ecommerce.orderservice.client.InventoryServiceClient;
import com.ecommerce.orderservice.client.OrderClientResponse;
import com.ecommerce.orderservice.client.OrderItemClientResponse;
import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.client.ProductClientResponse;
import com.ecommerce.orderservice.client.UserClient;
import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.dto.InventoryQuantityRequest;
import com.ecommerce.orderservice.dto.OrderItemRequest;
import com.ecommerce.orderservice.dto.OrderItemResponse;
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

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

	private final OrderRepository orderRepository;
	private final ProductClient productClient;
	private final InventoryServiceClient inventoryServiceClient;
	private final UserClient userClient;

	@Override
	@Transactional
	public OrderResponse createOrder(CreateOrderRequest request, Long currentUserId, String currentUserRole) {

		if (!"ADMIN".equals(currentUserRole) && !request.getUserId().equals(currentUserId)) {

			throw new ForbiddenException("You can only create orders for yourself.");
		}

		userClient.getUserById(request.getUserId());

		List<ValidatedOrderItem> validatedItems = validateOrderItems(request);

		Order order = buildOrder(request.getUserId(), validatedItems);

		reserveInventory(validatedItems);

		Order savedOrder = orderRepository.save(order);

		return mapToResponse(savedOrder);
	}

	private List<ValidatedOrderItem> validateOrderItems(CreateOrderRequest request) {

		List<ValidatedOrderItem> validatedItems = new ArrayList<>();

		for (OrderItemRequest itemRequest : request.getItems()) {

			ProductClientResponse productResponse = productClient.getProductById(itemRequest.getProductId());

			if (!productResponse.isActive()) {
				throw new InactiveProductException("The selected product is currently unavailable for ordering.");
			}

			InventoryClientResponse inventoryResponse = inventoryServiceClient
					.getInventoryByProductId(itemRequest.getProductId());

			if (inventoryResponse.getAvailableQuantity() < itemRequest.getQuantity()) {

				throw new InsufficientStockException(
						"Insufficient stock available for product with id: " + itemRequest.getProductId());
			}

			validatedItems.add(new ValidatedOrderItem(productResponse.getId(), itemRequest.getQuantity(),
					productResponse.getPrice()));
		}

		return validatedItems;
	}

	private Order buildOrder(Long userId, List<ValidatedOrderItem> validatedItems) {

		Order order = Order.builder().userId(userId).totalPrice(BigDecimal.ZERO).build();

		BigDecimal totalPrice = BigDecimal.ZERO;

		for (ValidatedOrderItem validatedItem : validatedItems) {

			BigDecimal subtotal = validatedItem.getUnitPrice()
					.multiply(BigDecimal.valueOf(validatedItem.getQuantity()));

			OrderItem orderItem = OrderItem.builder().productId(validatedItem.getProductId())
					.quantity(validatedItem.getQuantity()).unitPrice(validatedItem.getUnitPrice()).subtotal(subtotal)
					.build();

			order.addItem(orderItem);

			totalPrice = totalPrice.add(subtotal);
		}

		order.setTotalPrice(totalPrice);

		return order;
	}

	private void reserveInventory(List<ValidatedOrderItem> validatedItems) {

		List<ValidatedOrderItem> reservedItems = new ArrayList<>();

		try {

			for (ValidatedOrderItem validatedItem : validatedItems) {

				InventoryQuantityRequest request = InventoryQuantityRequest.builder()
						.productId(validatedItem.getProductId()).quantity(validatedItem.getQuantity()).build();

				inventoryServiceClient.reserveInventory(request);

				reservedItems.add(validatedItem);
			}

		} catch (Exception ex) {

			releaseReservedInventory(reservedItems);

			throw ex;
		}
	}

	private void releaseReservedInventory(List<ValidatedOrderItem> reservedItems) {

		for (ValidatedOrderItem item : reservedItems) {

			InventoryQuantityRequest request = InventoryQuantityRequest.builder().productId(item.getProductId())
					.quantity(item.getQuantity()).build();

			inventoryServiceClient.releaseInventory(request);
		}
	}

	@Override
	public OrderResponse getOrderById(Long id, Long currentUserId, String currentUserRole) {

		Order order = findOrderById(id);

		if (!"ADMIN".equals(currentUserRole) && !order.getUserId().equals(currentUserId)) {

			throw new ForbiddenException("You can only access your own orders.");
		}

		return mapToResponse(order);
	}

	@Override
	public List<OrderResponse> getAllOrders(String currentUserRole) {

		if (!"ADMIN".equals(currentUserRole)) {
			throw new ForbiddenException("Only administrators can access all orders.");
		}

		return orderRepository.findByDeletedAtIsNull().stream().map(this::mapToResponse).toList();
	}

	@Override
	public List<OrderResponse> getOrdersByUserId(Long userId, Long currentUserId, String currentUserRole) {

		if (!"ADMIN".equals(currentUserRole) && !userId.equals(currentUserId)) {

			throw new ForbiddenException("You can only access your own orders.");
		}

		return orderRepository.findByUserIdAndDeletedAtIsNull(userId).stream().map(this::mapToResponse).toList();
	}

	@Override
	@Transactional
	public void deleteOrder(Long id, String currentUserRole) {

		if (!"ADMIN".equals(currentUserRole)) {
			throw new ForbiddenException("Only administrators can delete orders.");
		}

		Order order = findOrderById(id);

		order.setDeletedAt(LocalDateTime.now());

		orderRepository.save(order);
	}

	@Override
	@Transactional
	public void confirmPayment(Long orderId) {

		Order order = findOrderById(orderId);

		if (order.getStatus() != OrderStatus.CREATED) {
			throw new InvalidOrderStateException("Only orders in CREATED status can be paid.");
		}

		for (OrderItem item : order.getItems()) {

			InventoryQuantityRequest request = InventoryQuantityRequest.builder().productId(item.getProductId())
					.quantity(item.getQuantity()).build();

			inventoryServiceClient.deductInventory(request);
		}

		order.setStatus(OrderStatus.PAID);

		orderRepository.save(order);
	}

	@Override
	@Transactional
	public OrderResponse cancelOrder(Long id, Long currentUserId, String currentUserRole) {

		Order order = findOrderById(id);

		if (!"ADMIN".equals(currentUserRole) && !order.getUserId().equals(currentUserId)) {

			throw new ForbiddenException("You can only cancel your own orders.");
		}

		if (order.getStatus() != OrderStatus.CREATED) {
			throw new InvalidOrderStateException("Only orders in CREATED status can be cancelled.");
		}

		for (OrderItem item : order.getItems()) {

			InventoryQuantityRequest request = InventoryQuantityRequest.builder().productId(item.getProductId())
					.quantity(item.getQuantity()).build();

			inventoryServiceClient.releaseInventory(request);
		}

		order.setStatus(OrderStatus.CANCELLED);

		Order updatedOrder = orderRepository.save(order);

		return mapToResponse(updatedOrder);
	}

	@Override
	public OrderClientResponse getInternalOrder(Long id) {

		Order order = findOrderById(id);

		return mapToClientResponse(order);
	}

	@Override
	public boolean hasPurchasedProduct(Long userId, Long productId) {

		return orderRepository.findByUserIdAndDeletedAtIsNull(userId).stream()
				.filter(order -> order.getStatus() == OrderStatus.PAID).flatMap(order -> order.getItems().stream())
				.anyMatch(item -> item.getProductId().equals(productId));
	}

	private Order findOrderById(Long id) {

		return orderRepository.findByIdAndDeletedAtIsNull(id)
				.orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
	}

	private OrderResponse mapToResponse(Order order) {

		List<OrderItemResponse> items = order.getItems().stream()
				.map(item -> OrderItemResponse.builder().productId(item.getProductId()).quantity(item.getQuantity())
						.unitPrice(item.getUnitPrice()).subtotal(item.getSubtotal()).build())
				.toList();

		return OrderResponse.builder().id(order.getId()).userId(order.getUserId()).items(items)
				.totalPrice(order.getTotalPrice()).status(order.getStatus()).createdAt(order.getCreatedAt())
				.updatedAt(order.getUpdatedAt()).build();
	}

	private OrderClientResponse mapToClientResponse(Order order) {

		List<OrderItemClientResponse> items = order.getItems().stream()
				.map(item -> OrderItemClientResponse.builder().productId(item.getProductId())
						.quantity(item.getQuantity()).unitPrice(item.getUnitPrice()).subtotal(item.getSubtotal())
						.build())
				.toList();

		OrderClientResponse response = new OrderClientResponse();

		response.setId(order.getId());
		response.setUserId(order.getUserId());
		response.setItems(items);
		response.setTotalPrice(order.getTotalPrice());
		response.setStatus(order.getStatus().name());

		return response;
	}
}