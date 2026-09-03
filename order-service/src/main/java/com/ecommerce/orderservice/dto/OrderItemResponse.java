package com.ecommerce.orderservice.dto;

import java.math.BigDecimal;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

	private Long productId;

	private Integer quantity;

	private BigDecimal unitPrice;

	private BigDecimal subtotal;

}
