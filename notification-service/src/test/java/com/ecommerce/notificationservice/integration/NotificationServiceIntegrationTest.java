package com.ecommerce.notificationservice.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;


import com.ecommerce.notificationservice.entity.Notification;
import com.ecommerce.notificationservice.entity.NotificationStatus;
import com.ecommerce.notificationservice.repository.NotificationRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationServiceIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private NotificationRepository notificationRepository;

	@BeforeEach
	void setUp() {
		notificationRepository.deleteAll();
	}


	private Notification createNotification(Long orderId, String message) {

		Notification notification = Notification.builder().orderId(orderId).message(message)
				.status(NotificationStatus.SENT).createdAt(LocalDateTime.now()).build();

		return notificationRepository.saveAndFlush(notification);
	}

	@Test
	void getAllNotifications_shouldReturnNotificationsForAdmin() throws Exception {

		createNotification(100L, "Payment completed for order 100.");
		createNotification(200L, "Payment completed for order 200.");

		mockMvc.perform(
				get("/api/notifications").header("X-User-Role", "ADMIN").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void getAllNotifications_shouldReturnEmptyListWhenNoNotificationsExist() throws Exception {

		mockMvc.perform(
				get("/api/notifications").header("X-User-Role", "ADMIN").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void getAllNotifications_shouldRejectCustomer() throws Exception {

		createNotification(100L, "Payment completed.");

		mockMvc.perform(
				get("/api/notifications").header("X-User-Role", "CUSTOMER").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
	}

	@Test
	void getNotificationById_shouldReturnNotificationForAdmin() throws Exception {

		Notification notification = createNotification(100L,
				"Payment for order #100 was successfully completed. Transaction ID: transaction-123");

		mockMvc.perform(get("/api/notifications/" + notification.getId()).header("X-User-Role", "ADMIN")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(notification.getId())).andExpect(jsonPath("$.orderId").value(100))
				.andExpect(jsonPath("$.status").value("SENT")).andExpect(jsonPath("$.message")
						.value("Payment for order #100 was successfully completed. Transaction ID: transaction-123"));
	}

	@Test
	void getNotificationById_shouldReturnNotFound() throws Exception {

		mockMvc.perform(
				get("/api/notifications/99999").header("X-User-Role", "ADMIN").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void getNotificationById_shouldRejectCustomer() throws Exception {

		Notification notification = createNotification(100L, "Payment completed.");

		mockMvc.perform(get("/api/notifications/" + notification.getId()).header("X-User-Role", "CUSTOMER")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403));
	}

	@Test
	void getNotificationsByOrderId_shouldReturnNotificationsForAdmin() throws Exception {

		createNotification(100L, "First notification.");
		createNotification(100L, "Second notification.");
		createNotification(200L, "Other order notification.");

		mockMvc.perform(get("/api/notifications/order/100").header("X-User-Role", "ADMIN")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].orderId").value(100))
				.andExpect(jsonPath("$[1].orderId").value(100));
	}

	@Test
	void getNotificationsByOrderId_shouldReturnEmptyListWhenNoNotificationsExist() throws Exception {

		mockMvc.perform(get("/api/notifications/order/99999").header("X-User-Role", "ADMIN")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void getNotificationsByOrderId_shouldRejectCustomer() throws Exception {

		createNotification(100L, "Payment completed.");

		mockMvc.perform(get("/api/notifications/order/100").header("X-User-Role", "CUSTOMER")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403));
	}

	@Test
	void getNotificationById_shouldPersistAndReturnCreatedNotification() throws Exception {

		Notification notification = createNotification(300L, "Payment for order #300 was successfully completed.");

		assertNotNull(notification.getId());

		Notification persisted = notificationRepository.findById(notification.getId()).orElseThrow();

		assertEquals(300L, persisted.getOrderId());
		assertEquals(NotificationStatus.SENT, persisted.getStatus());
		assertTrue(persisted.getCreatedAt() != null);
	}
}