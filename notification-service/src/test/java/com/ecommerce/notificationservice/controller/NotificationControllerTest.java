package com.ecommerce.notificationservice.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerce.notificationservice.dto.NotificationResponse;
import com.ecommerce.notificationservice.entity.NotificationStatus;
import com.ecommerce.notificationservice.exception.GlobalExceptionHandler;
import com.ecommerce.notificationservice.exception.NotificationNotFoundException;
import com.ecommerce.notificationservice.service.NotificationService;

@WebMvcTest(NotificationController.class)
@Import(GlobalExceptionHandler.class)
class NotificationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private NotificationService notificationService;

	private NotificationResponse createNotificationResponse() {

		return NotificationResponse.builder().id(1L).orderId(100L)
				.message("Payment for order #100 was successfully completed. Transaction ID: transaction-123")
				.status(NotificationStatus.SENT).createdAt(LocalDateTime.of(2026, 8, 15, 10, 0)).build();
	}

	@Test
	void getAllNotifications_shouldReturnNotificationsForAdmin() throws Exception {

		NotificationResponse notification = createNotificationResponse();

		when(notificationService.getAllNotifications()).thenReturn(List.of(notification));

		mockMvc.perform(
				get("/api/notifications").header("X-User-Role", "ADMIN").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(1)).andExpect(jsonPath("$[0].orderId").value(100))
				.andExpect(jsonPath("$[0].status").value("SENT"));
	}

	@Test
	void getAllNotifications_shouldRejectCustomer() throws Exception {

		mockMvc.perform(
				get("/api/notifications").header("X-User-Role", "CUSTOMER").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
	}

	@Test
	void getNotificationById_shouldReturnNotificationForAdmin() throws Exception {

		NotificationResponse notification = createNotificationResponse();

		when(notificationService.getNotificationById(1L)).thenReturn(notification);

		mockMvc.perform(
				get("/api/notifications/1").header("X-User-Role", "ADMIN").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.orderId").value(100))
				.andExpect(jsonPath("$.message")
						.value("Payment for order #100 was successfully completed. Transaction ID: transaction-123"))
				.andExpect(jsonPath("$.status").value("SENT"));
	}

	@Test
	void getNotificationById_shouldRejectCustomer() throws Exception {

		mockMvc.perform(
				get("/api/notifications/1").header("X-User-Role", "CUSTOMER").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
	}

	@Test
	void getNotificationById_shouldReturnNotFound() throws Exception {

		when(notificationService.getNotificationById(999L))
				.thenThrow(new NotificationNotFoundException("Notification not found with id: 999"));

		mockMvc.perform(
				get("/api/notifications/999").header("X-User-Role", "ADMIN").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("Notification not found with id: 999"));
	}

	@Test
	void getNotificationsByOrderId_shouldReturnNotificationsForAdmin() throws Exception {

		NotificationResponse notification = createNotificationResponse();

		when(notificationService.getNotificationsByOrderId(100L)).thenReturn(List.of(notification));

		mockMvc.perform(get("/api/notifications/order/100").header("X-User-Role", "ADMIN")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].orderId").value(100));
	}

	@Test
	void getNotificationsByOrderId_shouldRejectCustomer() throws Exception {

		mockMvc.perform(get("/api/notifications/order/100").header("X-User-Role", "CUSTOMER")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403));
	}

	@Test
	void getNotificationsByOrderId_shouldReturnEmptyList() throws Exception {

		when(notificationService.getNotificationsByOrderId(999L)).thenReturn(List.of());

		mockMvc.perform(get("/api/notifications/order/999").header("X-User-Role", "ADMIN")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}
}