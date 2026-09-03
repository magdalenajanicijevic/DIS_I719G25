package com.ecommerce.orderservice.listener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.orderservice.event.PaymentSuccessfulEvent;
import com.ecommerce.orderservice.service.OrderService;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

	@Mock
	private OrderService orderService;

	@Mock
	private PaymentSuccessfulEvent event;

	@InjectMocks
	private PaymentEventListener paymentEventListener;

	@Test
	void handlePaymentSuccessful_shouldConfirmPayment() {
		when(event.getOrderId()).thenReturn(100L);

		paymentEventListener.handlePaymentSuccessful(event);

		verify(orderService).confirmPayment(100L);
	}
}