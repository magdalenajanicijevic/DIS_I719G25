package com.ecommerce.notificationservice.integration;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.ecommerce.notificationservice.event.PaymentSuccessfulEvent;
import com.ecommerce.notificationservice.messaging.RabbitMQConstants;
import com.ecommerce.notificationservice.service.NotificationService;


@SpringBootTest(properties = {
	    "spring.rabbitmq.listener.simple.auto-startup=true",
	    "spring.rabbitmq.listener.direct.auto-startup=true"
	})
@Testcontainers
class PaymentEventRabbitMQIntegrationTest {
	
	@Container
	static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3-management");
	
	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
		registry.add("spring.rabbitmq.port", () -> rabbitMQ.getMappedPort(5672));
	}

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@MockitoBean
	private NotificationService notificationService;

	@Test
	void paymentSuccessfulEvent_shouldReachNotificationServiceListener() {

		PaymentSuccessfulEvent event = PaymentSuccessfulEvent.builder().orderId(999999L)
				.amount(new BigDecimal("250.00")).transactionId("rabbit-test-999999").paidAt(LocalDateTime.now())
				.build();

		rabbitTemplate.convertAndSend(RabbitMQConstants.PAYMENT_SUCCESS_QUEUE, event);

		verify(notificationService, timeout(5000)).createNotification(event);
	}
}