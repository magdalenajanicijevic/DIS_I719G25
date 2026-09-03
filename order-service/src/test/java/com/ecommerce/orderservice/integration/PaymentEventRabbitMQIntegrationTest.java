package com.ecommerce.orderservice.integration;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.ecommerce.orderservice.event.PaymentSuccessfulEvent;
import com.ecommerce.orderservice.messaging.RabbitMQConstants;
import com.ecommerce.orderservice.service.OrderService;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class PaymentEventRabbitMQIntegrationTest {

	@Container
	static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3.13-management");

	@DynamicPropertySource
	static void configureRabbitMQ(DynamicPropertyRegistry registry) {
	    registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
	    registry.add("spring.rabbitmq.port",
	            () -> rabbitMQ.getMappedPort(5672));
	    registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
	    registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);
	}

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@MockitoBean
	private OrderService orderService;

	@Test
	void paymentSuccessfulEvent_shouldReachOrderServiceListener() {

		PaymentSuccessfulEvent event = PaymentSuccessfulEvent.builder().orderId(999999L)
				.amount(new BigDecimal("250.00")).transactionId("rabbit-test-999999").paidAt(LocalDateTime.now())
				.build();

		rabbitTemplate.convertAndSend(RabbitMQConstants.PAYMENT_EXCHANGE, RabbitMQConstants.PAYMENT_SUCCESS_ROUTING_KEY,
				event);

		verify(orderService, timeout(5000)).confirmPayment(999999L);
	}
}