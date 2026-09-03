package com.ecommerce.paymentservice.messaging;

public class RabbitMQConstants {

	private RabbitMQConstants() {
	}

	public static final String PAYMENT_EXCHANGE = "payment.exchange";

	public static final String PAYMENT_SUCCESS_QUEUE = "payment.success.queue";

	public static final String PAYMENT_SUCCESS_ROUTING_KEY = "payment.success";

}
