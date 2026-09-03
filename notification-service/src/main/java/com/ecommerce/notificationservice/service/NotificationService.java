package com.ecommerce.notificationservice.service;

import java.util.List;

import com.ecommerce.notificationservice.dto.NotificationResponse;
import com.ecommerce.notificationservice.event.PaymentSuccessfulEvent;

public interface NotificationService {

    NotificationResponse getNotificationById(Long id);

    List<NotificationResponse> getAllNotifications();

    List<NotificationResponse> getNotificationsByOrderId(Long orderId);

    void createNotification(PaymentSuccessfulEvent event);

}