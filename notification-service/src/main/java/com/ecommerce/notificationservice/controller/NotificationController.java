package com.ecommerce.notificationservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.notificationservice.dto.NotificationResponse;
import com.ecommerce.notificationservice.exception.ForbiddenException;
import com.ecommerce.notificationservice.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;

	@GetMapping
	public ResponseEntity<List<NotificationResponse>> getAllNotifications(
			@RequestHeader("X-User-Role") String currentUserRole) {

		if (!"ADMIN".equals(currentUserRole)) {
			throw new ForbiddenException("Only administrators can access notifications.");
		}

		return ResponseEntity.ok(notificationService.getAllNotifications());
	}

	@GetMapping("/{id}")
	public ResponseEntity<NotificationResponse> getNotificationById(@PathVariable Long id,
			@RequestHeader("X-User-Role") String currentUserRole) {

		if (!"ADMIN".equals(currentUserRole)) {
			throw new ForbiddenException("Only administrators can access notifications.");
		}

		return ResponseEntity.ok(notificationService.getNotificationById(id));
	}

	@GetMapping("/order/{orderId}")
	public ResponseEntity<List<NotificationResponse>> getNotificationsByOrderId(@PathVariable Long orderId,
			@RequestHeader("X-User-Role") String currentUserRole) {

		if (!"ADMIN".equals(currentUserRole)) {
			throw new ForbiddenException("Only administrators can access notifications.");
		}

		return ResponseEntity.ok(notificationService.getNotificationsByOrderId(orderId));
	}
}