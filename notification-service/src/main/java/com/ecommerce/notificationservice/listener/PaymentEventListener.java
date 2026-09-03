package com.ecommerce.notificationservice.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.ecommerce.notificationservice.event.PaymentSuccessfulEvent;
import com.ecommerce.notificationservice.messaging.RabbitMQConstants;
import com.ecommerce.notificationservice.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

	private final NotificationService notificationService;

	@RabbitListener(queues = RabbitMQConstants.PAYMENT_SUCCESS_QUEUE)
	public void handlePaymentSuccessful(PaymentSuccessfulEvent event) {

		log.info("Received payment event for order {}", event.getOrderId());

		notificationService.createNotification(event);

		log.info("Notification created for order {}", event.getOrderId());
	}

}
