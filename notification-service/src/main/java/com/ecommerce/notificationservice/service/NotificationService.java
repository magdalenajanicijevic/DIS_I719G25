package com.ecommerce.notificationservice.service;

import java.util.List;

import com.ecommerce.notificationservice.dto.NotificationResponse;
import com.ecommerce.notificationservice.event.PaymentSuccessfulEvent;

public interface NotificationService {

    NotificationResponse getNotificationById(Long id, String currentUserRole);

    List<NotificationResponse> getAllNotifications(String currentUserRole);

    List<NotificationResponse> getNotificationsByOrderId(Long orderId, String currentUserRole);

    void createNotification(PaymentSuccessfulEvent event);

}