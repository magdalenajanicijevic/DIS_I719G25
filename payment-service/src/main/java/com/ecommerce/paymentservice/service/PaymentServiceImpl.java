package com.ecommerce.paymentservice.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ecommerce.paymentservice.client.OrderClient;
import com.ecommerce.paymentservice.client.OrderClientResponse;
import com.ecommerce.paymentservice.dto.CreatePaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.entity.PaymentStatus;
import com.ecommerce.paymentservice.event.PaymentSuccessfulEvent;
import com.ecommerce.paymentservice.exception.ForbiddenException;
import com.ecommerce.paymentservice.exception.InvalidPaymentStateException;
import com.ecommerce.paymentservice.exception.PaymentAlreadyExistsException;
import com.ecommerce.paymentservice.exception.PaymentNotFoundException;
import com.ecommerce.paymentservice.publisher.PaymentEventPublisher;
import com.ecommerce.paymentservice.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

	private final PaymentRepository paymentRepository;
	private final OrderClient orderClient;
	private final PaymentEventPublisher paymentEventPublisher;

	@Override
	public PaymentResponse createPayment(CreatePaymentRequest request, Long currentUserId, String currentUserRole) {

		OrderClientResponse order = orderClient.getOrderById(request.getOrderId());

		checkOrderAccess(order, currentUserId, currentUserRole);

		if (!"CREATED".equals(order.getStatus())) {
			throw new InvalidPaymentStateException("Only orders in CREATED status can be paid.");
		}

		if (paymentRepository.existsByOrderId(request.getOrderId())) {
			throw new PaymentAlreadyExistsException(
					"Payment already exists for order with id: " + request.getOrderId());
		}

		Payment payment = Payment.builder().orderId(order.getId()).amount(order.getTotalPrice())
				.paymentMethod(request.getPaymentMethod()).status(PaymentStatus.SUCCESS)
				.transactionId(UUID.randomUUID().toString()).build();

		Payment savedPayment = paymentRepository.save(payment);

		PaymentSuccessfulEvent event = PaymentSuccessfulEvent.builder().orderId(savedPayment.getOrderId())
				.amount(savedPayment.getAmount()).transactionId(savedPayment.getTransactionId())
				.paidAt(savedPayment.getCreatedAt()).build();

		paymentEventPublisher.publishPaymentSuccessful(event);

		return mapToResponse(savedPayment);
	}

	@Override
	public PaymentResponse getPaymentById(Long id, Long currentUserId, String currentUserRole) {

		Payment payment = findPaymentById(id);

		checkPaymentAccess(payment, currentUserId, currentUserRole);

		return mapToResponse(payment);
	}

	@Override
	public PaymentResponse getPaymentByOrderId(Long orderId, Long currentUserId, String currentUserRole) {

		Payment payment = paymentRepository.findByOrderId(orderId)
				.orElseThrow(() -> new PaymentNotFoundException("Payment not found for order id: " + orderId));

		checkPaymentAccess(payment, currentUserId, currentUserRole);

		return mapToResponse(payment);
	}

	@Override
	public List<PaymentResponse> getAllPayments(String currentUserRole) {

		if (!"ADMIN".equals(currentUserRole)) {
			throw new ForbiddenException("Only administrators can access all payments.");
		}

		return paymentRepository.findAll().stream().map(this::mapToResponse).toList();
	}

	@Override
	public PaymentResponse refundPayment(Long id, Long currentUserId, String currentUserRole) {

		Payment payment = findPaymentById(id);

		checkPaymentAccess(payment, currentUserId, currentUserRole);

		if (payment.getStatus() != PaymentStatus.SUCCESS) {
			throw new InvalidPaymentStateException("Only successful payments can be refunded.");
		}

		payment.setStatus(PaymentStatus.REFUNDED);

		Payment updatedPayment = paymentRepository.save(payment);

		return mapToResponse(updatedPayment);
	}

	private void checkOrderAccess(OrderClientResponse order, Long currentUserId, String currentUserRole) {

		if ("ADMIN".equals(currentUserRole)) {
			return;
		}

		if (!order.getUserId().equals(currentUserId)) {
			throw new ForbiddenException("You can only access your own orders.");
		}
	}

	private void checkPaymentAccess(Payment payment, Long currentUserId, String currentUserRole) {

		if ("ADMIN".equals(currentUserRole)) {
			return;
		}

		OrderClientResponse order = orderClient.getOrderById(payment.getOrderId());

		if (!order.getUserId().equals(currentUserId)) {
			throw new ForbiddenException("You can only access your own payments.");
		}
	}

	private Payment findPaymentById(Long id) {

		return paymentRepository.findById(id)
				.orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + id));
	}

	private PaymentResponse mapToResponse(Payment payment) {

		return PaymentResponse.builder().id(payment.getId()).orderId(payment.getOrderId()).amount(payment.getAmount())
				.paymentMethod(payment.getPaymentMethod()).status(payment.getStatus())
				.transactionId(payment.getTransactionId()).createdAt(payment.getCreatedAt())
				.updatedAt(payment.getUpdatedAt()).build();
	}

}