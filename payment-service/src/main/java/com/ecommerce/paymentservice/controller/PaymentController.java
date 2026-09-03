package com.ecommerce.paymentservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.ecommerce.paymentservice.dto.CreatePaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Validated
@Tag(name = "Payment Controller", description = "Operations related to payment management")
public class PaymentController {

	private final PaymentService paymentService;

	@PostMapping
	@Operation(summary = "Create payment")
	public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request,
			@RequestHeader("X-User-Id") Long currentUserId, @RequestHeader("X-User-Role") String currentUserRole) {

		PaymentResponse response = paymentService.createPayment(request, currentUserId, currentUserRole);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get payment by id")
	public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long id,
			@RequestHeader("X-User-Id") Long currentUserId, @RequestHeader("X-User-Role") String currentUserRole) {

		return ResponseEntity.ok(paymentService.getPaymentById(id, currentUserId, currentUserRole));
	}

	@GetMapping("/order/{orderId}")
	@Operation(summary = "Get payment by order id")
	public ResponseEntity<PaymentResponse> getPaymentByOrderId(@PathVariable Long orderId,
			@RequestHeader("X-User-Id") Long currentUserId, @RequestHeader("X-User-Role") String currentUserRole) {

		return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId, currentUserId, currentUserRole));
	}

	@GetMapping
	@Operation(summary = "Get all payments")
	public ResponseEntity<List<PaymentResponse>> getAllPayments(@RequestHeader("X-User-Role") String currentUserRole) {

		return ResponseEntity.ok(paymentService.getAllPayments(currentUserRole));
	}

	@PostMapping("/{id}/refund")
	@Operation(summary = "Refund payment")
	public ResponseEntity<PaymentResponse> refundPayment(@PathVariable Long id,
			@RequestHeader("X-User-Id") Long currentUserId, @RequestHeader("X-User-Role") String currentUserRole) {

		return ResponseEntity.ok(paymentService.refundPayment(id, currentUserId, currentUserRole));
	}

}