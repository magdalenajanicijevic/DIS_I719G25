package com.ecommerce.notificationservice.listener;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.notificationservice.event.PaymentSuccessfulEvent;
import com.ecommerce.notificationservice.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private PaymentEventListener paymentEventListener;

	@Test
	void handlePaymentSuccessful_shouldCreateNotification() {
		PaymentSuccessfulEvent event = PaymentSuccessfulEvent.builder().orderId(100L)
				.amount(new java.math.BigDecimal("250.00")).transactionId("transaction-123").build();

		paymentEventListener.handlePaymentSuccessful(event);

		verify(notificationService).createNotification(event);
	}
}