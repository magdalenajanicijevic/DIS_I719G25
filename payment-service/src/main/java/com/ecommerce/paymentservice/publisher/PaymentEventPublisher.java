package com.ecommerce.paymentservice.publisher;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.ecommerce.paymentservice.event.PaymentSuccessfulEvent;
import com.ecommerce.paymentservice.messaging.RabbitMQConstants;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentEventPublisher {

	private final RabbitTemplate rabbitTemplate;

	public void publishPaymentSuccessful(PaymentSuccessfulEvent event) {

		rabbitTemplate.convertAndSend(RabbitMQConstants.PAYMENT_EXCHANGE, RabbitMQConstants.PAYMENT_SUCCESS_ROUTING_KEY,
				event);
	}

}
