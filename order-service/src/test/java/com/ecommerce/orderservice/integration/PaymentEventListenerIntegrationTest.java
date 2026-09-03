package com.ecommerce.orderservice.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.ecommerce.orderservice.client.InventoryServiceClient;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.event.PaymentSuccessfulEvent;
import com.ecommerce.orderservice.messaging.RabbitMQConstants;
import com.ecommerce.orderservice.repository.OrderRepository;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class PaymentEventListenerIntegrationTest {

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

	@Autowired
	private OrderRepository orderRepository;

	@MockitoBean
	private InventoryServiceClient inventoryServiceClient;

	@BeforeEach
	void setUp() {
		orderRepository.deleteAll();
	}

	@Test
	void paymentSuccessfulEvent_shouldConfirmOrder() throws Exception {

		Order order = Order.builder().userId(1L).totalPrice(new BigDecimal("250.00")).status(OrderStatus.CREATED)
				.build();

		OrderItem item = OrderItem.builder().productId(100L).quantity(2).unitPrice(new BigDecimal("125.00"))
				.subtotal(new BigDecimal("250.00")).build();

		order.addItem(item);

		Order savedOrder = orderRepository.saveAndFlush(order);

		PaymentSuccessfulEvent event = new PaymentSuccessfulEvent(savedOrder.getId(), new BigDecimal("250.00"),
				"transaction-123", LocalDateTime.now());

		rabbitTemplate.convertAndSend(RabbitMQConstants.PAYMENT_EXCHANGE, RabbitMQConstants.PAYMENT_SUCCESS_ROUTING_KEY,
				event);

		long timeout = System.currentTimeMillis() + 5000;

		Order updatedOrder = null;

		while (System.currentTimeMillis() < timeout) {

			updatedOrder = orderRepository.findById(savedOrder.getId()).orElse(null);

			if (updatedOrder != null && updatedOrder.getStatus() == OrderStatus.PAID) {
				break;
			}

			Thread.sleep(100);
		}

		assertEquals(OrderStatus.PAID, updatedOrder.getStatus());

		verify(inventoryServiceClient).deductInventory(
				argThat(request -> request.getProductId().equals(100L) && request.getQuantity().equals(2)));
	}

	@Test
	void paymentSuccessfulEvent_shouldDeductInventoryForAllOrderItems() throws Exception {

		Order order = Order.builder().userId(1L).totalPrice(new BigDecimal("500.00")).status(OrderStatus.CREATED)
				.build();

		OrderItem firstItem = OrderItem.builder().productId(100L).quantity(2).unitPrice(new BigDecimal("100.00"))
				.subtotal(new BigDecimal("200.00")).build();

		OrderItem secondItem = OrderItem.builder().productId(200L).quantity(3).unitPrice(new BigDecimal("100.00"))
				.subtotal(new BigDecimal("300.00")).build();

		order.addItem(firstItem);
		order.addItem(secondItem);

		Order savedOrder = orderRepository.saveAndFlush(order);

		PaymentSuccessfulEvent event = new PaymentSuccessfulEvent(savedOrder.getId(), new BigDecimal("500.00"),
				"transaction-456", LocalDateTime.now());

		rabbitTemplate.convertAndSend(RabbitMQConstants.PAYMENT_EXCHANGE, RabbitMQConstants.PAYMENT_SUCCESS_ROUTING_KEY,
				event);

		long timeout = System.currentTimeMillis() + 5000;

		Order updatedOrder = null;

		while (System.currentTimeMillis() < timeout) {

			updatedOrder = orderRepository.findById(savedOrder.getId()).orElse(null);

			if (updatedOrder != null && updatedOrder.getStatus() == OrderStatus.PAID) {
				break;
			}

			Thread.sleep(100);
		}

		assertEquals(OrderStatus.PAID, updatedOrder.getStatus());

		verify(inventoryServiceClient).deductInventory(
				argThat(request -> request.getProductId().equals(100L) && request.getQuantity().equals(2)));

		verify(inventoryServiceClient).deductInventory(
				argThat(request -> request.getProductId().equals(200L) && request.getQuantity().equals(3)));
	}

	@Test
	void paymentSuccessfulEvent_shouldNotChangeAlreadyPaidOrder() throws Exception {

		Order order = Order.builder().userId(1L).totalPrice(new BigDecimal("250.00")).status(OrderStatus.PAID).build();

		OrderItem item = OrderItem.builder().productId(100L).quantity(2).unitPrice(new BigDecimal("125.00"))
				.subtotal(new BigDecimal("250.00")).build();

		order.addItem(item);

		Order savedOrder = orderRepository.saveAndFlush(order);

		PaymentSuccessfulEvent event = new PaymentSuccessfulEvent(savedOrder.getId(), new BigDecimal("250.00"),
				"transaction-789", LocalDateTime.now());

		rabbitTemplate.convertAndSend(RabbitMQConstants.PAYMENT_EXCHANGE, RabbitMQConstants.PAYMENT_SUCCESS_ROUTING_KEY,
				event);

		Thread.sleep(1000);

		Order updatedOrder = orderRepository.findById(savedOrder.getId()).orElseThrow();

		assertEquals(OrderStatus.PAID, updatedOrder.getStatus());
	}
}