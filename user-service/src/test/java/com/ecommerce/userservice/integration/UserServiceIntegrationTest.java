package com.ecommerce.userservice.integration;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerce.userservice.entity.Role;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.repository.UserRepository;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class UserServiceIntegrationTest {

	@SuppressWarnings("resource")
	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18").withDatabaseName("user_db")
			.withUsername("postgres").withPassword("postgres");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {

	    registry.add("spring.datasource.url", postgres::getJdbcUrl);
	    registry.add("spring.datasource.username", postgres::getUsername);
	    registry.add("spring.datasource.password", postgres::getPassword);
	    registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
	    registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
	}

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();
	}

	private User createUser(String firstName, String lastName, String email, Role role) {

		User user = User.builder().firstName(firstName).lastName(lastName).email(email)
				.password(passwordEncoder.encode("password123")).role(role).build();

		return userRepository.save(user);
	}

	@Test
	void register_shouldCreateCustomer() throws Exception {

		String request = """
				{
				    "firstName": "John",
				    "lastName": "Smith",
				    "email": "john.smith@example.com",
				    "password": "password123"
				}
				""";

		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(request))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.id", notNullValue()))
				.andExpect(jsonPath("$.firstName").value("John")).andExpect(jsonPath("$.lastName").value("Smith"))
				.andExpect(jsonPath("$.email").value("john.smith@example.com"))
				.andExpect(jsonPath("$.role").value("CUSTOMER"));
	}

	@Test
	void register_shouldRejectDuplicateEmail() throws Exception {

		String request = """
				{
				    "firstName": "John",
				    "lastName": "Smith",
				    "email": "john.smith@example.com",
				    "password": "password123"
				}
				""";

		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(request))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(request))
				.andExpect(status().isConflict());
	}

	@Test
	void register_shouldRejectInvalidRequest() throws Exception {

		String request = """
				{
				    "firstName": "",
				    "lastName": "Smith",
				    "email": "invalid-email",
				    "password": "123"
				}
				""";

		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(request))
				.andExpect(status().isBadRequest());
	}

	@Test
	void login_shouldReturnJwt() throws Exception {

		createUser("John", "Smith", "john.smith@example.com", Role.CUSTOMER);

		String request = """
				{
				    "email": "john.smith@example.com",
				    "password": "password123"
				}
				""";

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(request))
				.andExpect(status().isOk()).andExpect(jsonPath("$.token", notNullValue()));
	}

	@Test
	void login_shouldRejectWrongPassword() throws Exception {

		createUser("John", "Smith", "john.smith@example.com", Role.CUSTOMER);

		String request = """
				{
				    "email": "john.smith@example.com",
				    "password": "wrongpassword"
				}
				""";

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(request))
				.andExpect(status().isBadRequest());
	}

	@Test
	void getUserById_shouldAllowUserToAccessOwnData() throws Exception {

		User user = createUser("John", "Smith", "john.smith@example.com", Role.CUSTOMER);

		mockMvc.perform(
				get("/api/users/" + user.getId()).header("X-User-Id", user.getId()).header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(user.getId()))
				.andExpect(jsonPath("$.firstName").value("John")).andExpect(jsonPath("$.lastName").value("Smith"))
				.andExpect(jsonPath("$.email").value("john.smith@example.com"))
				.andExpect(jsonPath("$.role").value("CUSTOMER"));
	}

	@Test
	void getUserById_shouldAllowAdminToAccessAnyUser() throws Exception {

		User customer = createUser("John", "Smith", "john.smith@example.com", Role.CUSTOMER);

		User admin = createUser("Admin", "User", "admin@example.com", Role.ADMIN);

		mockMvc.perform(
				get("/api/users/" + customer.getId()).header("X-User-Id", admin.getId()).header("X-User-Role", "ADMIN"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(customer.getId()))
				.andExpect(jsonPath("$.email").value("john.smith@example.com"));
	}

	@Test
	void getUserById_shouldRejectCustomerAccessToAnotherUser() throws Exception {

		User customer = createUser("John", "Smith", "john.smith@example.com", Role.CUSTOMER);

		User anotherCustomer = createUser("Emily", "Johnson", "emily.johnson@example.com", Role.CUSTOMER);

		mockMvc.perform(get("/api/users/" + anotherCustomer.getId()).header("X-User-Id", customer.getId())
				.header("X-User-Role", "CUSTOMER")).andExpect(status().isForbidden());
	}

	@Test
	void getUserById_shouldReturnNotFound() throws Exception {

		User admin = createUser("Admin", "User", "admin@example.com", Role.ADMIN);

		mockMvc.perform(get("/api/users/99999").header("X-User-Id", admin.getId()).header("X-User-Role", "ADMIN"))
				.andExpect(status().isNotFound());
	}

	@Test
	void getAllUsers_shouldAllowAdmin() throws Exception {

		User admin = createUser("Admin", "User", "admin@example.com", Role.ADMIN);

		createUser("John", "Smith", "john.smith@example.com", Role.CUSTOMER);

		createUser("Emily", "Johnson", "emily.johnson@example.com", Role.CUSTOMER);

		mockMvc.perform(get("/api/users").header("X-User-Id", admin.getId()).header("X-User-Role", "ADMIN"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
	}

	@Test
	void getAllUsers_shouldRejectCustomer() throws Exception {

		User customer = createUser("John", "Smith", "john.smith@example.com", Role.CUSTOMER);

		mockMvc.perform(get("/api/users").header("X-User-Id", customer.getId()).header("X-User-Role", "CUSTOMER"))
				.andExpect(status().isForbidden());
	}

	@Test
	void createUser_shouldAllowAdmin() throws Exception {

		User admin = createUser("Admin", "User", "admin@example.com", Role.ADMIN);

		String request = """
				{
				    "firstName": "Michael",
				    "lastName": "Brown",
				    "email": "michael.brown@example.com",
				    "password": "password123",
				    "role": "CUSTOMER"
				}
				""";

		mockMvc.perform(post("/api/users").header("X-User-Id", admin.getId()).header("X-User-Role", "ADMIN")
				.contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isCreated())
				.andExpect(jsonPath("$.firstName").value("Michael")).andExpect(jsonPath("$.lastName").value("Brown"))
				.andExpect(jsonPath("$.email").value("michael.brown@example.com"))
				.andExpect(jsonPath("$.role").value("CUSTOMER"));
	}

	@Test
	void createUser_shouldRejectCustomer() throws Exception {

		User customer = createUser("John", "Smith", "john.smith@example.com", Role.CUSTOMER);

		String request = """
				{
				    "firstName": "Michael",
				    "lastName": "Brown",
				    "email": "michael.brown@example.com",
				    "password": "password123",
				    "role": "CUSTOMER"
				}
				""";

		mockMvc.perform(post("/api/users").header("X-User-Id", customer.getId()).header("X-User-Role", "CUSTOMER")
				.contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isForbidden());
	}

	@Test
	void updateUser_shouldAllowCustomerToUpdateOwnData() throws Exception {

		User customer = createUser("John", "Smith", "john.smith@example.com", Role.CUSTOMER);

		String request = """
				{
				    "firstName": "Michael",
				    "lastName": "Brown",
				    "email": "michael.brown@example.com",
				    "role": "CUSTOMER"
				}
				""";

		mockMvc.perform(put("/api/users/" + customer.getId()).header("X-User-Id", customer.getId())
				.header("X-User-Role", "CUSTOMER").contentType(MediaType.APPLICATION_JSON).content(request))
				.andExpect(status().isOk()).andExpect(jsonPath("$.firstName").value("Michael"))
				.andExpect(jsonPath("$.lastName").value("Brown"))
				.andExpect(jsonPath("$.email").value("michael.brown@example.com"))
				.andExpect(jsonPath("$.role").value("CUSTOMER"));
	}

	@Test
	void updateUser_shouldNotAllowCustomerToChangeRole() throws Exception {

		User customer = createUser("John", "Smith", "john.smith@example.com", Role.CUSTOMER);

		String request = """
				{
				    "firstName": "John",
				    "lastName": "Smith",
				    "email": "john.smith@example.com",
				    "role": "ADMIN"
				}
				""";

		mockMvc.perform(put("/api/users/" + customer.getId()).header("X-User-Id", customer.getId())
				.header("X-User-Role", "CUSTOMER").contentType(MediaType.APPLICATION_JSON).content(request))
				.andExpect(status().isOk()).andExpect(jsonPath("$.role").value("CUSTOMER"));
	}

	@Test
	void updateUser_shouldAllowAdminToChangeRole() throws Exception {

		User customer = createUser("John", "Smith", "john.smith@example.com", Role.CUSTOMER);

		User admin = createUser("Admin", "User", "admin@example.com", Role.ADMIN);

		String request = """
				{
				    "firstName": "John",
				    "lastName": "Smith",
				    "email": "john.smith@example.com",
				    "role": "ADMIN"
				}
				""";

		mockMvc.perform(put("/api/users/" + customer.getId()).header("X-User-Id", admin.getId())
				.header("X-User-Role", "ADMIN").contentType(MediaType.APPLICATION_JSON).content(request))
				.andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
	}

	@Test
	void updateUser_shouldRejectCustomerUpdatingAnotherUser() throws Exception {

		User customer = createUser("John", "Smith", "john.smith@example.com", Role.CUSTOMER);

		User anotherCustomer = createUser("Emily", "Johnson", "emily.johnson@example.com", Role.CUSTOMER);

		String request = """
				{
				    "firstName": "Michael",
				    "lastName": "Brown",
				    "email": "michael.brown@example.com",
				    "role": "CUSTOMER"
				}
				""";

		mockMvc.perform(put("/api/users/" + anotherCustomer.getId()).header("X-User-Id", customer.getId())
				.header("X-User-Role", "CUSTOMER").contentType(MediaType.APPLICATION_JSON).content(request))
				.andExpect(status().isForbidden());
	}

	@Test
	void deleteUser_shouldAllowAdmin() throws Exception {

		User admin = createUser("Admin", "User", "admin@example.com", Role.ADMIN);

		User customer = createUser("John", "Smith", "john.smith@example.com", Role.CUSTOMER);

		mockMvc.perform(delete("/api/users/" + customer.getId()).header("X-User-Id", admin.getId())
				.header("X-User-Role", "ADMIN")).andExpect(status().isNoContent());

		mockMvc.perform(
				get("/api/users/" + customer.getId()).header("X-User-Id", admin.getId()).header("X-User-Role", "ADMIN"))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteUser_shouldRejectCustomer() throws Exception {

		User customer = createUser("John", "Smith", "john.smith@example.com", Role.CUSTOMER);

		mockMvc.perform(delete("/api/users/" + customer.getId()).header("X-User-Id", customer.getId())
				.header("X-User-Role", "CUSTOMER")).andExpect(status().isForbidden());
	}
}