package com.ecommerce.orderservice.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.ecommerce.orderservice.event.PaymentSuccessfulEvent;
import com.ecommerce.orderservice.messaging.RabbitMQConstants;
import com.ecommerce.orderservice.service.OrderService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentEventListener {

	private final OrderService orderService;

	@RabbitListener(queues = RabbitMQConstants.PAYMENT_SUCCESS_QUEUE)
	public void handlePaymentSuccessful(PaymentSuccessfulEvent event) {

		orderService.confirmPayment(event.getOrderId());
	}

}
