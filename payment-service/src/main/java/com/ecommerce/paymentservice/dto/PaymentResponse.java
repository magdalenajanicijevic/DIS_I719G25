package com.ecommerce.paymentservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ecommerce.paymentservice.entity.PaymentMethod;
import com.ecommerce.paymentservice.entity.PaymentStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResponse {

	private Long id;

	private Long orderId;

	private BigDecimal amount;

	private PaymentMethod paymentMethod;

	private PaymentStatus status;

	private String transactionId;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

}
