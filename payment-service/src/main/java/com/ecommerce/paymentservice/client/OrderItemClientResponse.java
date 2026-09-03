package com.ecommerce.paymentservice.client;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemClientResponse {

	private Long productId;

	private Integer quantity;

	private BigDecimal unitPrice;

	private BigDecimal subtotal;

}