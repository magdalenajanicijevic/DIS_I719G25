package com.ecommerce.notificationservice.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.ecommerce.notificationservice.entity.Notification;
import com.ecommerce.notificationservice.entity.NotificationStatus;
import com.ecommerce.notificationservice.event.PaymentSuccessfulEvent;
import com.ecommerce.notificationservice.messaging.RabbitMQConstants;
import com.ecommerce.notificationservice.repository.NotificationRepository;

@SpringBootTest(properties = {
	    "spring.rabbitmq.listener.simple.auto-startup=true",
	    "spring.rabbitmq.listener.direct.auto-startup=true"
	})
@Testcontainers
class PaymentEventListenerIntegrationTest {
	
	@Container
	static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3-management");
	
	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
		registry.add("spring.rabbitmq.port", () -> rabbitMQ.getMappedPort(5672));
	}

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private NotificationRepository notificationRepository;

	@BeforeEach
	void setUp() {
		notificationRepository.deleteAll();
	}

	@Test
	void paymentSuccessfulEvent_shouldCreateNotification() throws Exception {

		PaymentSuccessfulEvent event = PaymentSuccessfulEvent.builder().orderId(100L).amount(new BigDecimal("250.00"))
				.transactionId("transaction-123").paidAt(LocalDateTime.now()).build();

		rabbitTemplate.convertAndSend(RabbitMQConstants.PAYMENT_EXCHANGE, RabbitMQConstants.PAYMENT_SUCCESS_ROUTING_KEY,
				event);

		long timeout = System.currentTimeMillis() + 5000;

		Notification notification = null;

		while (System.currentTimeMillis() < timeout) {

			notification = notificationRepository.findAll().stream().filter(n -> n.getOrderId().equals(100L))
					.findFirst().orElse(null);

			if (notification != null) {
				break;
			}

			Thread.sleep(100);
		}

		assertNotNull(notification);
		assertEquals(100L, notification.getOrderId());
		assertEquals(NotificationStatus.SENT, notification.getStatus());
		assertEquals("Payment for order #100 was successfully completed. Transaction ID: transaction-123",
				notification.getMessage());
	}

	@Test
	void paymentSuccessfulEvent_shouldCreateNotificationsForDifferentOrders() throws Exception {

		PaymentSuccessfulEvent firstEvent = PaymentSuccessfulEvent.builder().orderId(100L)
				.amount(new BigDecimal("100.00")).transactionId("transaction-100").paidAt(LocalDateTime.now()).build();

		PaymentSuccessfulEvent secondEvent = PaymentSuccessfulEvent.builder().orderId(200L)
				.amount(new BigDecimal("200.00")).transactionId("transaction-200").paidAt(LocalDateTime.now()).build();

		rabbitTemplate.convertAndSend(RabbitMQConstants.PAYMENT_EXCHANGE, RabbitMQConstants.PAYMENT_SUCCESS_ROUTING_KEY,
				firstEvent);

		rabbitTemplate.convertAndSend(RabbitMQConstants.PAYMENT_EXCHANGE, RabbitMQConstants.PAYMENT_SUCCESS_ROUTING_KEY,
				secondEvent);

		long timeout = System.currentTimeMillis() + 5000;

		while (System.currentTimeMillis() < timeout) {

			long count = notificationRepository.findAll().stream()
					.filter(n -> n.getOrderId().equals(100L) || n.getOrderId().equals(200L)).count();

			if (count == 2) {
				break;
			}

			Thread.sleep(100);
		}

		assertEquals(2, notificationRepository.findAll().stream()
				.filter(n -> n.getOrderId().equals(100L) || n.getOrderId().equals(200L)).count());
	}

	@Test
	void paymentSuccessfulEvent_shouldStoreTransactionIdInMessage() throws Exception {

		PaymentSuccessfulEvent event = PaymentSuccessfulEvent.builder().orderId(500L).amount(new BigDecimal("500.00"))
				.transactionId("unique-transaction-500").paidAt(LocalDateTime.now()).build();

		rabbitTemplate.convertAndSend(RabbitMQConstants.PAYMENT_EXCHANGE, RabbitMQConstants.PAYMENT_SUCCESS_ROUTING_KEY,
				event);

		long timeout = System.currentTimeMillis() + 5000;

		Notification notification = null;

		while (System.currentTimeMillis() < timeout) {

			notification = notificationRepository.findAll().stream().filter(n -> n.getOrderId().equals(500L))
					.findFirst().orElse(null);

			if (notification != null) {
				break;
			}

			Thread.sleep(100);
		}

		assertNotNull(notification);
		assertEquals("Payment for order #500 was successfully completed. Transaction ID: unique-transaction-500",
				notification.getMessage());
	}
}