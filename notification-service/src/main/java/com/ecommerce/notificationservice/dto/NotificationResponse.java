package com.ecommerce.notificationservice.dto;

import java.time.LocalDateTime;

import com.ecommerce.notificationservice.entity.NotificationStatus;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

	private Long id;

	private Long orderId;

	private String message;

	private NotificationStatus status;

	private LocalDateTime createdAt;

}