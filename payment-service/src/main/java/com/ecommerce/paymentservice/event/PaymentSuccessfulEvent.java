package com.ecommerce.paymentservice.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentSuccessfulEvent implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Long orderId;

	private BigDecimal amount;

	private String transactionId;

	private LocalDateTime paidAt;

}
