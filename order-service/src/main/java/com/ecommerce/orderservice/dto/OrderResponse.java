package com.ecommerce.orderservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.ecommerce.orderservice.entity.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

	private Long id;

	private Long userId;

	private List<OrderItemResponse> items;

	private BigDecimal totalPrice;

	private OrderStatus status;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

}