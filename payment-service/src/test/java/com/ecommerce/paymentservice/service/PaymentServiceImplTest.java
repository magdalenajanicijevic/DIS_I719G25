package com.ecommerce.paymentservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.paymentservice.client.OrderClient;
import com.ecommerce.paymentservice.client.OrderClientResponse;
import com.ecommerce.paymentservice.dto.CreatePaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.entity.PaymentMethod;
import com.ecommerce.paymentservice.entity.PaymentStatus;
import com.ecommerce.paymentservice.event.PaymentSuccessfulEvent;
import com.ecommerce.paymentservice.exception.ForbiddenException;
import com.ecommerce.paymentservice.exception.InvalidPaymentStateException;
import com.ecommerce.paymentservice.exception.PaymentAlreadyExistsException;
import com.ecommerce.paymentservice.exception.PaymentNotFoundException;
import com.ecommerce.paymentservice.publisher.PaymentEventPublisher;
import com.ecommerce.paymentservice.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private OrderClient orderClient;

	@Mock
	private PaymentEventPublisher paymentEventPublisher;

	@InjectMocks
	private PaymentServiceImpl paymentService;

	private OrderClientResponse order;
	private Payment payment;
	private CreatePaymentRequest request;

	@BeforeEach
	void setUp() {
		order = new OrderClientResponse();
		order.setId(100L);
		order.setUserId(10L);
		order.setTotalPrice(new BigDecimal("250.00"));
		order.setStatus("CREATED");

		request = new CreatePaymentRequest();
		request.setOrderId(100L);
		request.setPaymentMethod(PaymentMethod.CARD);

		payment = Payment.builder().id(1L).orderId(100L).amount(new BigDecimal("250.00"))
				.paymentMethod(PaymentMethod.CARD).status(PaymentStatus.SUCCESS).transactionId("transaction-123")
				.createdAt(LocalDateTime.of(2026, 8, 14, 10, 0)).updatedAt(LocalDateTime.of(2026, 8, 14, 10, 0))
				.build();
	}

	@Test
	void createPayment_shouldCreatePaymentSuccessfullyForCustomer() {
		when(orderClient.getOrderById(100L)).thenReturn(order);
		when(paymentRepository.existsByOrderId(100L)).thenReturn(false);
		when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

		PaymentResponse response = paymentService.createPayment(request, 10L, "CUSTOMER");

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals(100L, response.getOrderId());
		assertEquals(new BigDecimal("250.00"), response.getAmount());
		assertEquals(PaymentMethod.CARD, response.getPaymentMethod());
		assertEquals(PaymentStatus.SUCCESS, response.getStatus());
		assertEquals("transaction-123", response.getTransactionId());

		verify(orderClient).getOrderById(100L);
		verify(paymentRepository).existsByOrderId(100L);
		verify(paymentRepository).save(any(Payment.class));
		verify(paymentEventPublisher).publishPaymentSuccessful(any(PaymentSuccessfulEvent.class));
	}

	@Test
	void createPayment_shouldCreatePaymentSuccessfullyForAdmin() {
		order.setUserId(20L);

		when(orderClient.getOrderById(100L)).thenReturn(order);
		when(paymentRepository.existsByOrderId(100L)).thenReturn(false);
		when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

		PaymentResponse response = paymentService.createPayment(request, 10L, "ADMIN");

		assertNotNull(response);
		assertEquals(100L, response.getOrderId());

		verify(orderClient).getOrderById(100L);
		verify(paymentRepository).save(any(Payment.class));
		verify(paymentEventPublisher).publishPaymentSuccessful(any(PaymentSuccessfulEvent.class));
	}

	@Test
	void createPayment_shouldRejectCustomerForAnotherUsersOrder() {
		order.setUserId(20L);

		when(orderClient.getOrderById(100L)).thenReturn(order);

		ForbiddenException exception = assertThrows(ForbiddenException.class,
				() -> paymentService.createPayment(request, 10L, "CUSTOMER"));

		assertEquals("You can only access your own orders.", exception.getMessage());

		verify(orderClient).getOrderById(100L);
		verify(paymentRepository, never()).existsByOrderId(any());
		verify(paymentRepository, never()).save(any(Payment.class));
		verifyNoInteractions(paymentEventPublisher);
	}

	@Test
	void createPayment_shouldThrowExceptionWhenOrderIsNotCreated() {
		order.setStatus("PAID");

		when(orderClient.getOrderById(100L)).thenReturn(order);

		InvalidPaymentStateException exception = assertThrows(InvalidPaymentStateException.class,
				() -> paymentService.createPayment(request, 10L, "CUSTOMER"));

		assertEquals("Only orders in CREATED status can be paid.", exception.getMessage());

		verify(orderClient).getOrderById(100L);
		verify(paymentRepository, never()).existsByOrderId(any());
		verify(paymentRepository, never()).save(any(Payment.class));
		verifyNoInteractions(paymentEventPublisher);
	}

	@Test
	void createPayment_shouldThrowExceptionWhenPaymentAlreadyExists() {
		when(orderClient.getOrderById(100L)).thenReturn(order);
		when(paymentRepository.existsByOrderId(100L)).thenReturn(true);

		PaymentAlreadyExistsException exception = assertThrows(PaymentAlreadyExistsException.class,
				() -> paymentService.createPayment(request, 10L, "CUSTOMER"));

		assertEquals("Payment already exists for order with id: 100", exception.getMessage());

		verify(orderClient).getOrderById(100L);
		verify(paymentRepository).existsByOrderId(100L);
		verify(paymentRepository, never()).save(any(Payment.class));
		verifyNoInteractions(paymentEventPublisher);
	}

	@Test
	void createPayment_shouldCreatePaymentWithCorrectData() {
		when(orderClient.getOrderById(100L)).thenReturn(order);
		when(paymentRepository.existsByOrderId(100L)).thenReturn(false);
		when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

		paymentService.createPayment(request, 10L, "CUSTOMER");

		ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

		verify(paymentRepository).save(paymentCaptor.capture());

		Payment savedPayment = paymentCaptor.getValue();

		assertEquals(100L, savedPayment.getOrderId());
		assertEquals(new BigDecimal("250.00"), savedPayment.getAmount());
		assertEquals(PaymentMethod.CARD, savedPayment.getPaymentMethod());
		assertEquals(PaymentStatus.SUCCESS, savedPayment.getStatus());
		assertNotNull(savedPayment.getTransactionId());
	}

	@Test
	void createPayment_shouldPublishCorrectEvent() {
		when(orderClient.getOrderById(100L)).thenReturn(order);
		when(paymentRepository.existsByOrderId(100L)).thenReturn(false);
		when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

		paymentService.createPayment(request, 10L, "CUSTOMER");

		ArgumentCaptor<PaymentSuccessfulEvent> eventCaptor = ArgumentCaptor.forClass(PaymentSuccessfulEvent.class);

		verify(paymentEventPublisher).publishPaymentSuccessful(eventCaptor.capture());

		PaymentSuccessfulEvent event = eventCaptor.getValue();

		assertEquals(100L, event.getOrderId());
		assertEquals(new BigDecimal("250.00"), event.getAmount());
		assertEquals("transaction-123", event.getTransactionId());
		assertEquals(payment.getCreatedAt(), event.getPaidAt());
	}

	@Test
	void getPaymentById_shouldReturnOwnPaymentForCustomer() {
		when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
		when(orderClient.getOrderById(100L)).thenReturn(order);

		PaymentResponse response = paymentService.getPaymentById(1L, 10L, "CUSTOMER");

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals(100L, response.getOrderId());
		assertEquals(new BigDecimal("250.00"), response.getAmount());
		assertEquals(PaymentMethod.CARD, response.getPaymentMethod());
		assertEquals(PaymentStatus.SUCCESS, response.getStatus());
		assertEquals("transaction-123", response.getTransactionId());

		verify(paymentRepository).findById(1L);
		verify(orderClient).getOrderById(100L);
	}

	@Test
	void getPaymentById_shouldReturnPaymentForAdmin() {
		when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

		PaymentResponse response = paymentService.getPaymentById(1L, 99L, "ADMIN");

		assertNotNull(response);
		assertEquals(1L, response.getId());

		verify(paymentRepository).findById(1L);
		verifyNoInteractions(orderClient);
	}

	@Test
	void getPaymentById_shouldRejectCustomerForAnotherUsersPayment() {
		order.setUserId(20L);

		when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
		when(orderClient.getOrderById(100L)).thenReturn(order);

		ForbiddenException exception = assertThrows(ForbiddenException.class,
				() -> paymentService.getPaymentById(1L, 10L, "CUSTOMER"));

		assertEquals("You can only access your own payments.", exception.getMessage());

		verify(paymentRepository).findById(1L);
		verify(orderClient).getOrderById(100L);
	}

	@Test
	void getPaymentById_shouldThrowExceptionWhenPaymentNotFound() {
		when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

		PaymentNotFoundException exception = assertThrows(PaymentNotFoundException.class,
				() -> paymentService.getPaymentById(1L, 10L, "CUSTOMER"));

		assertEquals("Payment not found with id: 1", exception.getMessage());

		verify(paymentRepository).findById(1L);
		verifyNoInteractions(orderClient);
	}

	@Test
	void getPaymentByOrderId_shouldReturnOwnPaymentForCustomer() {
		when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.of(payment));
		when(orderClient.getOrderById(100L)).thenReturn(order);

		PaymentResponse response = paymentService.getPaymentByOrderId(100L, 10L, "CUSTOMER");

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals(100L, response.getOrderId());
		assertEquals(PaymentStatus.SUCCESS, response.getStatus());

		verify(paymentRepository).findByOrderId(100L);
		verify(orderClient).getOrderById(100L);
	}

	@Test
	void getPaymentByOrderId_shouldReturnPaymentForAdmin() {
		when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.of(payment));

		PaymentResponse response = paymentService.getPaymentByOrderId(100L, 99L, "ADMIN");

		assertNotNull(response);
		assertEquals(1L, response.getId());

		verify(paymentRepository).findByOrderId(100L);
		verifyNoInteractions(orderClient);
	}

	@Test
	void getPaymentByOrderId_shouldRejectCustomerForAnotherUsersPayment() {
		order.setUserId(20L);

		when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.of(payment));
		when(orderClient.getOrderById(100L)).thenReturn(order);

		ForbiddenException exception = assertThrows(ForbiddenException.class,
				() -> paymentService.getPaymentByOrderId(100L, 10L, "CUSTOMER"));

		assertEquals("You can only access your own payments.", exception.getMessage());

		verify(paymentRepository).findByOrderId(100L);
		verify(orderClient).getOrderById(100L);
	}

	@Test
	void getPaymentByOrderId_shouldThrowExceptionWhenPaymentNotFound() {
		when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.empty());

		PaymentNotFoundException exception = assertThrows(PaymentNotFoundException.class,
				() -> paymentService.getPaymentByOrderId(100L, 10L, "CUSTOMER"));

		assertEquals("Payment not found for order id: 100", exception.getMessage());

		verify(paymentRepository).findByOrderId(100L);
		verifyNoInteractions(orderClient);
	}

	@Test
	void getAllPayments_shouldReturnAllPaymentsForAdmin() {
		Payment secondPayment = Payment.builder().id(2L).orderId(200L).amount(new BigDecimal("500.00"))
				.paymentMethod(PaymentMethod.PAYPAL).status(PaymentStatus.SUCCESS).transactionId("transaction-456")
				.build();

		when(paymentRepository.findAll()).thenReturn(List.of(payment, secondPayment));

		List<PaymentResponse> response = paymentService.getAllPayments("ADMIN");

		assertNotNull(response);
		assertEquals(2, response.size());
		assertEquals(1L, response.get(0).getId());
		assertEquals(2L, response.get(1).getId());

		verify(paymentRepository).findAll();
	}

	@Test
	void getAllPayments_shouldReturnEmptyListForAdminWhenNoPaymentsExist() {
		when(paymentRepository.findAll()).thenReturn(List.of());

		List<PaymentResponse> response = paymentService.getAllPayments("ADMIN");

		assertNotNull(response);
		assertEquals(0, response.size());

		verify(paymentRepository).findAll();
	}

	@Test
	void getAllPayments_shouldRejectCustomer() {
		ForbiddenException exception = assertThrows(ForbiddenException.class,
				() -> paymentService.getAllPayments("CUSTOMER"));

		assertEquals("Only administrators can access all payments.", exception.getMessage());

		verifyNoInteractions(paymentRepository);
	}

	@Test
	void refundPayment_shouldRefundOwnSuccessfulPaymentForCustomer() {
		when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
		when(orderClient.getOrderById(100L)).thenReturn(order);
		when(paymentRepository.save(payment)).thenReturn(payment);

		PaymentResponse response = paymentService.refundPayment(1L, 10L, "CUSTOMER");

		assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
		assertNotNull(response);
		assertEquals(PaymentStatus.REFUNDED, response.getStatus());

		verify(paymentRepository).findById(1L);
		verify(orderClient).getOrderById(100L);
		verify(paymentRepository).save(payment);
	}

	@Test
	void refundPayment_shouldRefundAnySuccessfulPaymentForAdmin() {
		when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
		when(paymentRepository.save(payment)).thenReturn(payment);

		PaymentResponse response = paymentService.refundPayment(1L, 99L, "ADMIN");

		assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
		assertNotNull(response);
		assertEquals(PaymentStatus.REFUNDED, response.getStatus());

		verify(paymentRepository).findById(1L);
		verify(paymentRepository).save(payment);
		verifyNoInteractions(orderClient);
	}

	@Test
	void refundPayment_shouldRejectCustomerForAnotherUsersPayment() {
		order.setUserId(20L);

		when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
		when(orderClient.getOrderById(100L)).thenReturn(order);

		ForbiddenException exception = assertThrows(ForbiddenException.class,
				() -> paymentService.refundPayment(1L, 10L, "CUSTOMER"));

		assertEquals("You can only access your own payments.", exception.getMessage());

		verify(paymentRepository).findById(1L);
		verify(orderClient).getOrderById(100L);
		verify(paymentRepository, never()).save(any(Payment.class));
	}

	@Test
	void refundPayment_shouldThrowExceptionWhenPaymentIsPending() {
		payment.setStatus(PaymentStatus.PENDING);

		when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
		when(orderClient.getOrderById(100L)).thenReturn(order);

		InvalidPaymentStateException exception = assertThrows(InvalidPaymentStateException.class,
				() -> paymentService.refundPayment(1L, 10L, "CUSTOMER"));

		assertEquals("Only successful payments can be refunded.", exception.getMessage());

		verify(paymentRepository).findById(1L);
		verify(orderClient).getOrderById(100L);
		verify(paymentRepository, never()).save(any(Payment.class));
	}

	@Test
	void refundPayment_shouldThrowExceptionWhenPaymentIsFailed() {
		payment.setStatus(PaymentStatus.FAILED);

		when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
		when(orderClient.getOrderById(100L)).thenReturn(order);

		InvalidPaymentStateException exception = assertThrows(InvalidPaymentStateException.class,
				() -> paymentService.refundPayment(1L, 10L, "CUSTOMER"));

		assertEquals("Only successful payments can be refunded.", exception.getMessage());

		verify(paymentRepository).findById(1L);
		verify(orderClient).getOrderById(100L);
		verify(paymentRepository, never()).save(any(Payment.class));
	}

	@Test
	void refundPayment_shouldThrowExceptionWhenPaymentIsAlreadyRefunded() {
		payment.setStatus(PaymentStatus.REFUNDED);

		when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
		when(orderClient.getOrderById(100L)).thenReturn(order);

		InvalidPaymentStateException exception = assertThrows(InvalidPaymentStateException.class,
				() -> paymentService.refundPayment(1L, 10L, "CUSTOMER"));

		assertEquals("Only successful payments can be refunded.", exception.getMessage());

		verify(paymentRepository).findById(1L);
		verify(orderClient).getOrderById(100L);
		verify(paymentRepository, never()).save(any(Payment.class));
	}

	@Test
	void refundPayment_shouldThrowExceptionWhenPaymentNotFound() {
		when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

		PaymentNotFoundException exception = assertThrows(PaymentNotFoundException.class,
				() -> paymentService.refundPayment(1L, 10L, "CUSTOMER"));

		assertEquals("Payment not found with id: 1", exception.getMessage());

		verify(paymentRepository).findById(1L);
		verify(paymentRepository, never()).save(any(Payment.class));
		verifyNoInteractions(orderClient);
	}
}