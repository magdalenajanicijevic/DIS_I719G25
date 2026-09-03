package com.ecommerce.paymentservice.service;

import java.util.List;

import com.ecommerce.paymentservice.dto.CreatePaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;

public interface PaymentService {

	PaymentResponse createPayment(CreatePaymentRequest request, Long currentUserId, String currentUserRole);

	PaymentResponse getPaymentById(Long id, Long currentUserId, String currentUserRole);

	PaymentResponse getPaymentByOrderId(Long orderId, Long currentUserId, String currentUserRole);

	List<PaymentResponse> getAllPayments(String currentUserRole);

	PaymentResponse refundPayment(Long id, Long currentUserId, String currentUserRole);

}