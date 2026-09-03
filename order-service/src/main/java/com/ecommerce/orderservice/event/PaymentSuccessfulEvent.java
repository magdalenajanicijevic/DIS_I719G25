package com.ecommerce.orderservice.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSuccessfulEvent {

	private Long orderId;
	private BigDecimal amount;
	private String transactionId;
	private LocalDateTime paidAt;

}
