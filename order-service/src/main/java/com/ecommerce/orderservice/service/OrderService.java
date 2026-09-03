package com.ecommerce.orderservice.service;

import java.util.List;

import com.ecommerce.orderservice.client.OrderClientResponse;
import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.dto.OrderResponse;

public interface OrderService {

	OrderResponse createOrder(CreateOrderRequest request, Long currentUserId, String currentUserRole);

	OrderResponse getOrderById(Long id, Long currentUserId, String currentUserRole);

	List<OrderResponse> getAllOrders(String currentUserRole);

	List<OrderResponse> getOrdersByUserId(Long userId, Long currentUserId, String currentUserRole);

	void deleteOrder(Long id, String currentUserRole);

	void confirmPayment(Long orderId);

	OrderResponse cancelOrder(Long id, Long currentUserId, String currentUserRole);

	OrderClientResponse getInternalOrder(Long id);

	boolean hasPurchasedProduct(Long userId, Long productId);
}