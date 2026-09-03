package com.ecommerce.userservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.ecommerce.commonsecurity.security.JwtService;
import com.ecommerce.userservice.dto.LoginRequest;
import com.ecommerce.userservice.dto.LoginResponse;
import com.ecommerce.userservice.dto.RegisterRequest;
import com.ecommerce.userservice.dto.UserResponse;
import com.ecommerce.userservice.entity.Role;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.exception.BadRequestException;
import com.ecommerce.userservice.exception.EmailAlreadyExistsException;
import com.ecommerce.userservice.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AuthServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtService jwtService;

	@InjectMocks
	private AuthServiceImpl authService;

	private User user;
	private LoginRequest loginRequest;
	private RegisterRequest registerRequest;

	@BeforeEach
	void setUp() {

		user = User.builder().id(1L).firstName("John").lastName("Smith").email("john.smith@example.com")
				.password("encodedPassword").role(Role.CUSTOMER).build();

		loginRequest = new LoginRequest("john.smith@example.com", "password123");

		registerRequest = RegisterRequest.builder().firstName("John").lastName("Smith").email("john.smith@example.com")
				.password("password123").build();
	}

	@Test
	void login_shouldReturnJwtToken_whenCredentialsAreValid() {

		when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(user));

		when(passwordEncoder.matches(loginRequest.password(), user.getPassword())).thenReturn(true);

		when(jwtService.generateToken(user.getEmail(), user.getId(), user.getRole().name())).thenReturn("jwt-token");

		LoginResponse response = authService.login(loginRequest);

		assertNotNull(response);
		assertEquals("jwt-token", response.token());

		verify(userRepository).findByEmail(loginRequest.email());

		verify(passwordEncoder).matches(loginRequest.password(), user.getPassword());

		verify(jwtService).generateToken(user.getEmail(), user.getId(), user.getRole().name());
	}

	@Test
	void login_shouldThrowException_whenEmailDoesNotExist() {

		when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.empty());

		BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.login(loginRequest));

		assertEquals("Invalid email or password.", exception.getMessage());

		verify(userRepository).findByEmail(loginRequest.email());

		verify(passwordEncoder, never()).matches(any(), any());

		verify(jwtService, never()).generateToken(any(), any(), any());
	}

	@Test
	void login_shouldThrowException_whenPasswordIsIncorrect() {

		when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(user));

		when(passwordEncoder.matches(loginRequest.password(), user.getPassword())).thenReturn(false);

		BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.login(loginRequest));

		assertEquals("Invalid email or password.", exception.getMessage());

		verify(userRepository).findByEmail(loginRequest.email());

		verify(passwordEncoder).matches(loginRequest.password(), user.getPassword());

		verify(jwtService, never()).generateToken(any(), any(), any());
	}

	@Test
	void login_shouldGenerateTokenWithCorrectUserData() {

		when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(user));

		when(passwordEncoder.matches(loginRequest.password(), user.getPassword())).thenReturn(true);

		when(jwtService.generateToken(user.getEmail(), user.getId(), user.getRole().name())).thenReturn("jwt-token");

		authService.login(loginRequest);

		verify(jwtService).generateToken("john.smith@example.com", 1L, "CUSTOMER");
	}

	@Test
	void register_shouldCreateCustomerSuccessfully() {

		when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);

		when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");

		when(userRepository.save(any(User.class))).thenReturn(user);

		UserResponse response = authService.register(registerRequest);

		assertNotNull(response);
		assertEquals(user.getId(), response.getId());
		assertEquals(user.getFirstName(), response.getFirstName());
		assertEquals(user.getLastName(), response.getLastName());
		assertEquals(user.getEmail(), response.getEmail());
		assertEquals(Role.CUSTOMER, response.getRole());

		verify(userRepository).existsByEmail(registerRequest.getEmail());

		verify(passwordEncoder).encode(registerRequest.getPassword());

		verify(userRepository).save(any(User.class));
	}

	@Test
	void register_shouldAlwaysAssignCustomerRole() {

		when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);

		when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");

		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UserResponse response = authService.register(registerRequest);

		assertEquals(Role.CUSTOMER, response.getRole());

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

		verify(userRepository).save(userCaptor.capture());

		User savedUser = userCaptor.getValue();

		assertEquals(Role.CUSTOMER, savedUser.getRole());
	}

	@Test
	void register_shouldEncodePasswordBeforeSavingUser() {

		when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);

		when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");

		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		authService.register(registerRequest);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

		verify(userRepository).save(userCaptor.capture());

		User savedUser = userCaptor.getValue();

		assertEquals("encodedPassword", savedUser.getPassword());

		verify(passwordEncoder).encode(registerRequest.getPassword());
	}

	@Test
	void register_shouldThrowException_whenEmailAlreadyExists() {

		when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

		EmailAlreadyExistsException exception = assertThrows(EmailAlreadyExistsException.class,
				() -> authService.register(registerRequest));

		assertEquals("Email already exists.", exception.getMessage());

		verify(userRepository).existsByEmail(registerRequest.getEmail());

		verify(passwordEncoder, never()).encode(any());

		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void register_shouldSaveCorrectUserData() {

		when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);

		when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");

		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		authService.register(registerRequest);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

		verify(userRepository).save(userCaptor.capture());

		User savedUser = userCaptor.getValue();

		assertEquals(registerRequest.getFirstName(), savedUser.getFirstName());

		assertEquals(registerRequest.getLastName(), savedUser.getLastName());

		assertEquals(registerRequest.getEmail(), savedUser.getEmail());

		assertEquals("encodedPassword", savedUser.getPassword());

		assertEquals(Role.CUSTOMER, savedUser.getRole());
	}
}