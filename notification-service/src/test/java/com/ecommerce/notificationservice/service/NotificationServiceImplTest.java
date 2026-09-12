package com.ecommerce.notificationservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.ecommerce.notificationservice.exception.ForbiddenException;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ecommerce.notificationservice.dto.NotificationResponse;
import com.ecommerce.notificationservice.entity.Notification;
import com.ecommerce.notificationservice.entity.NotificationStatus;
import com.ecommerce.notificationservice.event.PaymentSuccessfulEvent;
import com.ecommerce.notificationservice.exception.NotificationNotFoundException;
import com.ecommerce.notificationservice.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

	@Mock
	private NotificationRepository notificationRepository;

	@InjectMocks
	private NotificationServiceImpl notificationService;

	private Notification notification;
	private PaymentSuccessfulEvent event;
	private LocalDateTime createdAt;

	@BeforeEach
	void setUp() {
		createdAt = LocalDateTime.of(2026, 8, 14, 10, 0);

		event = PaymentSuccessfulEvent.builder().orderId(100L).transactionId("transaction-123").build();

		notification = Notification.builder().id(1L).orderId(100L)
				.message("Payment for order #100 was successfully completed. Transaction ID: transaction-123")
				.status(NotificationStatus.SENT).createdAt(createdAt).build();
	}

	@Test
	void createNotification_shouldCreateNotificationSuccessfully() {
		notificationService.createNotification(event);

		ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

		verify(notificationRepository).save(captor.capture());

		Notification savedNotification = captor.getValue();

		assertEquals(100L, savedNotification.getOrderId());
		assertEquals("Payment for order #100 was successfully completed. Transaction ID: transaction-123",
				savedNotification.getMessage());
		assertEquals(NotificationStatus.SENT, savedNotification.getStatus());
	}

	@Test
	void createNotification_shouldBuildCorrectMessage() {
		notificationService.createNotification(event);

		ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

		verify(notificationRepository).save(captor.capture());

		Notification savedNotification = captor.getValue();

		assertEquals("Payment for order #100 was successfully completed. Transaction ID: transaction-123",
				savedNotification.getMessage());
	}

	@Test
	void getNotificationById_shouldReturnNotificationSuccessfully() {
		when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

		NotificationResponse response = notificationService.getNotificationById(1L, "ADMIN");

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals(100L, response.getOrderId());
		assertEquals("Payment for order #100 was successfully completed. Transaction ID: transaction-123",
				response.getMessage());
		assertEquals(NotificationStatus.SENT, response.getStatus());
		assertEquals(createdAt, response.getCreatedAt());

		verify(notificationRepository).findById(1L);
	}

	@Test
	void getNotificationById_shouldThrowException_whenNotificationNotFound() {
		when(notificationRepository.findById(1L)).thenReturn(Optional.empty());

		NotificationNotFoundException exception = assertThrows(NotificationNotFoundException.class,
				() -> notificationService.getNotificationById(1L, "ADMIN"));

		assertEquals("Notification not found with id: 1", exception.getMessage());

		verify(notificationRepository).findById(1L);
		verify(notificationRepository, never()).save(any(Notification.class));
	}

	@Test
	void getAllNotifications_shouldReturnAllNotifications() {
		Notification secondNotification = Notification.builder().id(2L).orderId(200L)
				.message("Payment for order #200 was successfully completed. Transaction ID: transaction-456")
				.status(NotificationStatus.SENT).createdAt(createdAt).build();

		when(notificationRepository.findAll()).thenReturn(List.of(notification, secondNotification));

		List<NotificationResponse> response = notificationService.getAllNotifications("ADMIN");

		assertNotNull(response);
		assertEquals(2, response.size());

		assertEquals(1L, response.get(0).getId());
		assertEquals(2L, response.get(1).getId());

		assertEquals(100L, response.get(0).getOrderId());
		assertEquals(200L, response.get(1).getOrderId());

		verify(notificationRepository).findAll();
	}

	@Test
	void getAllNotifications_shouldReturnEmptyList_whenNoNotificationsExist() {
		when(notificationRepository.findAll()).thenReturn(List.of());

		List<NotificationResponse> response = notificationService.getAllNotifications("ADMIN");

		assertNotNull(response);
		assertEquals(0, response.size());

		verify(notificationRepository).findAll();
	}

	@Test
	void getNotificationsByOrderId_shouldReturnNotifications() {
		Notification secondNotification = Notification.builder().id(2L).orderId(100L).message("Another notification")
				.status(NotificationStatus.SENT).createdAt(createdAt).build();

		when(notificationRepository.findByOrderId(100L)).thenReturn(List.of(notification, secondNotification));

		List<NotificationResponse> response = notificationService.getNotificationsByOrderId(100L, "ADMIN");

		assertNotNull(response);
		assertEquals(2, response.size());

		assertEquals(1L, response.get(0).getId());
		assertEquals(2L, response.get(1).getId());

		assertEquals(100L, response.get(0).getOrderId());
		assertEquals(100L, response.get(1).getOrderId());

		verify(notificationRepository).findByOrderId(100L);
	}

	@Test
	void getNotificationsByOrderId_shouldReturnEmptyList_whenNoNotificationsExist() {
		when(notificationRepository.findByOrderId(100L)).thenReturn(List.of());

		List<NotificationResponse> response = notificationService.getNotificationsByOrderId(100L, "ADMIN");

		assertNotNull(response);
		assertEquals(0, response.size());

		verify(notificationRepository).findByOrderId(100L);
	}

	@Test
	void getAllNotifications_shouldThrowForbidden_whenCustomer() {
		ForbiddenException exception = assertThrows(ForbiddenException.class,
				() -> notificationService.getAllNotifications("CUSTOMER"));

		assertEquals("Only administrators can access notifications.", exception.getMessage());

		verifyNoInteractions(notificationRepository);
	}

	@Test
	void getNotificationById_shouldThrowForbidden_whenCustomer() {
		ForbiddenException exception = assertThrows(ForbiddenException.class,
				() -> notificationService.getNotificationById(1L, "CUSTOMER"));

		assertEquals("Only administrators can access notifications.", exception.getMessage());

		verifyNoInteractions(notificationRepository);
	}

	@Test
	void getNotificationsByOrderId_shouldThrowForbidden_whenCustomer() {
		ForbiddenException exception = assertThrows(ForbiddenException.class,
				() -> notificationService.getNotificationsByOrderId(100L, "CUSTOMER"));

		assertEquals("Only administrators can access notifications.", exception.getMessage());

		verifyNoInteractions(notificationRepository);
	}
}