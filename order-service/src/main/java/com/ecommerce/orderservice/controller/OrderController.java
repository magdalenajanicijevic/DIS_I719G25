package com.ecommerce.orderservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.orderservice.client.OrderClientResponse;
import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
@Tag(name = "Order Controller", description = "Operations related to order management")
public class OrderController {

	private final OrderService orderService;

	@PostMapping
	@Operation(summary = "Create order")
	public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request,
			@RequestHeader("X-User-Id") Long currentUserId, @RequestHeader("X-User-Role") String currentUserRole) {

		OrderResponse response = orderService.createOrder(request, currentUserId, currentUserRole);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PostMapping("/{id}/cancel")
	@Operation(summary = "Cancel order")
	public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id,
			@RequestHeader("X-User-Id") Long currentUserId, @RequestHeader("X-User-Role") String currentUserRole) {

		return ResponseEntity.ok(orderService.cancelOrder(id, currentUserId, currentUserRole));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get order by id")
	public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id,
			@RequestHeader("X-User-Id") Long currentUserId, @RequestHeader("X-User-Role") String currentUserRole) {

		return ResponseEntity.ok(orderService.getOrderById(id, currentUserId, currentUserRole));
	}

	@GetMapping
	@Operation(summary = "Get all orders")
	public ResponseEntity<List<OrderResponse>> getAllOrders(@RequestHeader("X-User-Role") String currentUserRole) {

		return ResponseEntity.ok(orderService.getAllOrders(currentUserRole));
	}

	@GetMapping("/user/{userId}")
	@Operation(summary = "Get all orders for a user")
	public ResponseEntity<List<OrderResponse>> getOrdersByUserId(@PathVariable Long userId,
			@RequestHeader("X-User-Id") Long currentUserId, @RequestHeader("X-User-Role") String currentUserRole) {

		return ResponseEntity.ok(orderService.getOrdersByUserId(userId, currentUserId, currentUserRole));
	}

	@GetMapping("/internal/{id}")
	@Operation(summary = "Get order by id (internal)")
	public ResponseEntity<OrderClientResponse> getOrderByIdInternal(@PathVariable Long id) {

		return ResponseEntity.ok(orderService.getInternalOrder(id));
	}

	@GetMapping("/internal/purchased")
	public boolean hasPurchasedProduct(@RequestParam Long userId, @RequestParam Long productId) {

		return orderService.hasPurchasedProduct(userId, productId);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete order")
	public ResponseEntity<Void> deleteOrder(@PathVariable Long id,
			@RequestHeader("X-User-Role") String currentUserRole) {

		orderService.deleteOrder(id, currentUserRole);

		return ResponseEntity.noContent().build();
	}
}