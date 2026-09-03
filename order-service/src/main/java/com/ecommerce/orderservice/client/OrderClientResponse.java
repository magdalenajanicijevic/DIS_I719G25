package com.ecommerce.orderservice.client;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class OrderClientResponse {

    private Long id;

    private Long userId;

    private List<OrderItemClientResponse> items;

    private BigDecimal totalPrice;

    private String status;

}
