package com.ecommerce.paymentservice.publisher;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.ecommerce.paymentservice.event.PaymentSuccessfulEvent;
import com.ecommerce.paymentservice.messaging.RabbitMQConstants;

@ExtendWith(MockitoExtension.class)
class PaymentEventPublisherTest {

	@Mock
	private RabbitTemplate rabbitTemplate;

	@Mock
	private PaymentSuccessfulEvent event;

	@InjectMocks
	private PaymentEventPublisher paymentEventPublisher;

	@Test
	void publishPaymentSuccessful_shouldPublishEvent() {
		paymentEventPublisher.publishPaymentSuccessful(event);

		verify(rabbitTemplate).convertAndSend(RabbitMQConstants.PAYMENT_EXCHANGE,
				RabbitMQConstants.PAYMENT_SUCCESS_ROUTING_KEY, event);
	}
}