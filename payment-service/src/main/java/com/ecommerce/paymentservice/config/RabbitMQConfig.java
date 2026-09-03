package com.ecommerce.paymentservice.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ecommerce.paymentservice.messaging.RabbitMQConstants;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;


@Configuration
public class RabbitMQConfig {

	@Bean
	DirectExchange paymentExchange() {
		return new DirectExchange(RabbitMQConstants.PAYMENT_EXCHANGE);
	}

	@Bean
	Queue paymentSuccessQueue() {
		return QueueBuilder.durable(RabbitMQConstants.PAYMENT_SUCCESS_QUEUE).build();
	}

	@Bean
	Binding paymentSuccessBinding() {
		return BindingBuilder.bind(paymentSuccessQueue()).to(paymentExchange()).with(RabbitMQConstants.PAYMENT_SUCCESS_ROUTING_KEY);
	}

	@Bean
	MessageConverter messageConverter() {
		return new JacksonJsonMessageConverter();
	}

}
