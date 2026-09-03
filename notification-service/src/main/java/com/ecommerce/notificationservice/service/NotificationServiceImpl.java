package com.ecommerce.notificationservice.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ecommerce.notificationservice.dto.NotificationResponse;
import com.ecommerce.notificationservice.entity.Notification;
import com.ecommerce.notificationservice.entity.NotificationStatus;
import com.ecommerce.notificationservice.event.PaymentSuccessfulEvent;
import com.ecommerce.notificationservice.exception.NotificationNotFoundException;
import com.ecommerce.notificationservice.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

	private final NotificationRepository notificationRepository;

	@Override
	public void createNotification(PaymentSuccessfulEvent event) {

		Notification notification = Notification.builder().orderId(event.getOrderId()).message(buildMessage(event))
				.status(NotificationStatus.SENT).build();

		notificationRepository.save(notification);
	}

	@Override
	public NotificationResponse getNotificationById(Long id) {

		Notification notification = findNotificationById(id);

		return mapToResponse(notification);
	}

	@Override
	public List<NotificationResponse> getAllNotifications() {

		return notificationRepository.findAll().stream().map(this::mapToResponse).toList();
	}

	@Override
	public List<NotificationResponse> getNotificationsByOrderId(Long orderId) {

		return notificationRepository.findByOrderId(orderId).stream().map(this::mapToResponse).toList();
	}

	private Notification findNotificationById(Long id) {

		return notificationRepository.findById(id)
				.orElseThrow(() -> new NotificationNotFoundException("Notification not found with id: " + id));
	}

	private NotificationResponse mapToResponse(Notification notification) {

		return NotificationResponse.builder().id(notification.getId()).orderId(notification.getOrderId())
				.message(notification.getMessage()).status(notification.getStatus())
				.createdAt(notification.getCreatedAt()).build();
	}

	private String buildMessage(PaymentSuccessfulEvent event) {

		return String.format("Payment for order #%d was successfully completed. Transaction ID: %s", event.getOrderId(),
				event.getTransactionId());
	}

}
