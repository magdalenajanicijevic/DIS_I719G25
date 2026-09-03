package com.ecommerce.orderservice.service;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ValidatedOrderItem {

    private Long productId;

    private Integer quantity;

    private BigDecimal unitPrice;

}
