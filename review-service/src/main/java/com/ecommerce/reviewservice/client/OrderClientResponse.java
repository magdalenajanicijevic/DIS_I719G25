package com.ecommerce.reviewservice.client;

import java.math.BigDecimal;

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
public class OrderClientResponse {

	private Long id;

	private Long userId;

	private Long productId;

	private Integer quantity;

	private BigDecimal totalPrice;

	private String status;

}