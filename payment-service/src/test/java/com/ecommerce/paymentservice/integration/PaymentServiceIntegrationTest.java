package com.ecommerce.paymentservice.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.paymentservice.client.OrderClient;
import com.ecommerce.paymentservice.client.OrderClientResponse;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.entity.PaymentMethod;
import com.ecommerce.paymentservice.entity.PaymentStatus;
import com.ecommerce.paymentservice.publisher.PaymentEventPublisher;
import com.ecommerce.paymentservice.repository.PaymentRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class PaymentServiceIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PaymentRepository paymentRepository;

	@MockitoBean
	private OrderClient orderClient;

	@MockitoBean
	private PaymentEventPublisher paymentEventPublisher;

	private OrderClientResponse customerOrder;
	private OrderClientResponse anotherUserOrder;

	@BeforeEach
	void setUp() {
		paymentRepository.deleteAll();

		customerOrder = new OrderClientResponse();
		customerOrder.setId(100L);
		customerOrder.setUserId(1L);
		customerOrder.setTotalPrice(new BigDecimal("250.00"));
		customerOrder.setStatus("CREATED");

		anotherUserOrder = new OrderClientResponse();
		anotherUserOrder.setId(200L);
		anotherUserOrder.setUserId(2L);
		anotherUserOrder.setTotalPrice(new BigDecimal("300.00"));
		anotherUserOrder.setStatus("CREATED");
	}

	@Test
	void createPayment_shouldCreatePaymentForCustomerOwnOrder() throws Exception {

		when(orderClient.getOrderById(100L)).thenReturn(customerOrder);

		mockMvc.perform(post("/api/payments").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER")
				.contentType(MediaType.APPLICATION_JSON).content("""
						{
							"orderId": 100,
							"paymentMethod": "CARD"
						}
						""")).andExpect(status().isCreated()).andExpect(jsonPath("$.orderId").value(100))
				.andExpect(jsonPath("$.amount").value(250.00)).andExpect(jsonPath("$.paymentMethod").value("CARD"))
				.andExpect(jsonPath("$.status").value("SUCCESS"));
	}

	@Test
	void createPayment_shouldCreatePaymentForAdmin() throws Exception {

		when(orderClient.getOrderById(200L)).thenReturn(anotherUserOrder);

		mockMvc.perform(post("/api/payments").header("X-User-Id", "1").header("X-User-Role", "ADMIN")
				.contentType(MediaType.APPLICATION_JSON).content("""
						{
							"orderId": 200,
							"paymentMethod": "PAYPAL"
						}
						""")).andExpect(status().isCreated()).andExpect(jsonPath("$.orderId").value(200))
				.andExpect(jsonPath("$.paymentMethod").value("PAYPAL"))
				.andExpect(jsonPath("$.status").value("SUCCESS"));
	}

	@Test
	void createPayment_shouldRejectCustomerForAnotherUsersOrder() throws Exception {

		when(orderClient.getOrderById(200L)).thenReturn(anotherUserOrder);

		mockMvc.perform(post("/api/payments").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER")
				.contentType(MediaType.APPLICATION_JSON).content("""
						{
							"orderId": 200,
							"paymentMethod": "CARD"
						}
						""")).andExpect(status().isForbidden());
	}

	@Test
	void createPayment_shouldRejectPaymentForNonCreatedOrder() throws Exception {

		customerOrder.setStatus("PAID");

		when(orderClient.getOrderById(100L)).thenReturn(customerOrder);

		mockMvc.perform(post("/api/payments").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER")
				.contentType(MediaType.APPLICATION_JSON).content("""
						{
							"orderId": 100,
							"paymentMethod": "CARD"
						}
						""")).andExpect(status().isBadRequest());
	}

	@Test
	void createPayment_shouldRejectDuplicatePayment() throws Exception {

		when(orderClient.getOrderById(100L)).thenReturn(customerOrder);

		Payment payment = Payment.builder().orderId(100L).amount(new BigDecimal("250.00"))
				.paymentMethod(PaymentMethod.CARD).status(PaymentStatus.SUCCESS).transactionId("transaction-existing")
				.build();

		paymentRepository.save(payment);

		mockMvc.perform(post("/api/payments").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER")
				.contentType(MediaType.APPLICATION_JSON).content("""
						{
							"orderId": 100,
							"paymentMethod": "CARD"
						}
						""")).andExpect(status().isConflict());
	}

	@Test
	void getPaymentById_shouldReturnOwnPaymentForCustomer() throws Exception {

		Payment payment = paymentRepository
				.save(Payment.builder().orderId(100L).amount(new BigDecimal("250.00")).paymentMethod(PaymentMethod.CARD)
						.status(PaymentStatus.SUCCESS).transactionId("transaction-100").build());

		when(orderClient.getOrderById(100L)).thenReturn(customerOrder);

		mockMvc.perform(
				get("/api/payments/" + payment.getId()).header("X-User-Id", "1").header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(payment.getId()))
				.andExpect(jsonPath("$.orderId").value(100)).andExpect(jsonPath("$.status").value("SUCCESS"));
	}

	@Test
	void getPaymentById_shouldReturnPaymentForAdmin() throws Exception {

		Payment payment = paymentRepository.save(
				Payment.builder().orderId(200L).amount(new BigDecimal("300.00")).paymentMethod(PaymentMethod.PAYPAL)
						.status(PaymentStatus.SUCCESS).transactionId("transaction-200").build());

		mockMvc.perform(get("/api/payments/" + payment.getId()).header("X-User-Id", "1").header("X-User-Role", "ADMIN"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(payment.getId()))
				.andExpect(jsonPath("$.orderId").value(200));
	}

	@Test
	void getPaymentById_shouldRejectCustomerForAnotherUsersPayment() throws Exception {

		Payment payment = paymentRepository
				.save(Payment.builder().orderId(200L).amount(new BigDecimal("300.00")).paymentMethod(PaymentMethod.CARD)
						.status(PaymentStatus.SUCCESS).transactionId("transaction-200").build());

		when(orderClient.getOrderById(200L)).thenReturn(anotherUserOrder);

		mockMvc.perform(
				get("/api/payments/" + payment.getId()).header("X-User-Id", "1").header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isForbidden());
	}

	@Test
	void getPaymentById_shouldReturnNotFound() throws Exception {

		mockMvc.perform(get("/api/payments/99999").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isNotFound());
	}

	@Test
	void getPaymentByOrderId_shouldReturnOwnPaymentForCustomer() throws Exception {

		Payment payment = paymentRepository
				.save(Payment.builder().orderId(100L).amount(new BigDecimal("250.00")).paymentMethod(PaymentMethod.CARD)
						.status(PaymentStatus.SUCCESS).transactionId("transaction-100").build());

		when(orderClient.getOrderById(100L)).thenReturn(customerOrder);

		mockMvc.perform(get("/api/payments/order/100").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(payment.getId()))
				.andExpect(jsonPath("$.orderId").value(100));
	}

	@Test
	void getPaymentByOrderId_shouldRejectCustomerForAnotherUsersPayment() throws Exception {

		paymentRepository
				.save(Payment.builder().orderId(200L).amount(new BigDecimal("300.00")).paymentMethod(PaymentMethod.CARD)
						.status(PaymentStatus.SUCCESS).transactionId("transaction-200").build());

		when(orderClient.getOrderById(200L)).thenReturn(anotherUserOrder);

		mockMvc.perform(get("/api/payments/order/200").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isForbidden());
	}

	@Test
	void getPaymentByOrderId_shouldReturnNotFound() throws Exception {

		mockMvc.perform(get("/api/payments/order/99999").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isNotFound());
	}

	@Test
	void getAllPayments_shouldReturnAllPaymentsForAdmin() throws Exception {

		paymentRepository
				.save(Payment.builder().orderId(100L).amount(new BigDecimal("250.00")).paymentMethod(PaymentMethod.CARD)
						.status(PaymentStatus.SUCCESS).transactionId("transaction-100").build());

		paymentRepository.save(
				Payment.builder().orderId(200L).amount(new BigDecimal("300.00")).paymentMethod(PaymentMethod.PAYPAL)
						.status(PaymentStatus.SUCCESS).transactionId("transaction-200").build());

		mockMvc.perform(get("/api/payments").header("X-User-Id", "1").header("X-User-Role", "ADMIN"))
				.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
	}

	@Test
	void getAllPayments_shouldRejectCustomer() throws Exception {

		paymentRepository
				.save(Payment.builder().orderId(100L).amount(new BigDecimal("250.00")).paymentMethod(PaymentMethod.CARD)
						.status(PaymentStatus.SUCCESS).transactionId("transaction-100").build());

		mockMvc.perform(get("/api/payments").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isForbidden());
	}

	@Test
	void refundPayment_shouldRefundOwnPaymentForCustomer() throws Exception {

		Payment payment = paymentRepository
				.save(Payment.builder().orderId(100L).amount(new BigDecimal("250.00")).paymentMethod(PaymentMethod.CARD)
						.status(PaymentStatus.SUCCESS).transactionId("transaction-100").build());

		when(orderClient.getOrderById(100L)).thenReturn(customerOrder);

		mockMvc.perform(post("/api/payments/" + payment.getId() + "/refund").header("X-User-Id", "1")
				.header("X-User-Role", "CUSTOMER")).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(payment.getId())).andExpect(jsonPath("$.status").value("REFUNDED"));
	}

	@Test
	void refundPayment_shouldRefundAnyPaymentForAdmin() throws Exception {

		Payment payment = paymentRepository.save(
				Payment.builder().orderId(200L).amount(new BigDecimal("300.00")).paymentMethod(PaymentMethod.PAYPAL)
						.status(PaymentStatus.SUCCESS).transactionId("transaction-200").build());

		mockMvc.perform(post("/api/payments/" + payment.getId() + "/refund").header("X-User-Id", "1")
				.header("X-User-Role", "ADMIN")).andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REFUNDED"));
	}

	@Test
	void refundPayment_shouldRejectCustomerForAnotherUsersPayment() throws Exception {

		Payment payment = paymentRepository
				.save(Payment.builder().orderId(200L).amount(new BigDecimal("300.00")).paymentMethod(PaymentMethod.CARD)
						.status(PaymentStatus.SUCCESS).transactionId("transaction-200").build());

		when(orderClient.getOrderById(200L)).thenReturn(anotherUserOrder);

		mockMvc.perform(post("/api/payments/" + payment.getId() + "/refund").header("X-User-Id", "1")
				.header("X-User-Role", "CUSTOMER")).andExpect(status().isForbidden());
	}

	@Test
	void refundPayment_shouldRejectNonSuccessfulPayment() throws Exception {

		Payment payment = paymentRepository
				.save(Payment.builder().orderId(100L).amount(new BigDecimal("250.00")).paymentMethod(PaymentMethod.CARD)
						.status(PaymentStatus.REFUNDED).transactionId("transaction-refunded").build());

		when(orderClient.getOrderById(100L)).thenReturn(customerOrder);

		mockMvc.perform(post("/api/payments/" + payment.getId() + "/refund").header("X-User-Id", "1")
				.header("X-User-Role", "CUSTOMER")).andExpect(status().isBadRequest());
	}

	@Test
	void refundPayment_shouldReturnNotFound() throws Exception {

		mockMvc.perform(post("/api/payments/99999/refund").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isNotFound());
	}

	@Test
	void createPayment_shouldRejectInvalidRequest() throws Exception {

		mockMvc.perform(post("/api/payments").header("X-User-Id", "1").header("X-User-Role", "CUSTOMER")
				.contentType(MediaType.APPLICATION_JSON).content("""
						{
							"orderId": null,
							"paymentMethod": null
						}
						""")).andExpect(status().isBadRequest());
	}
}