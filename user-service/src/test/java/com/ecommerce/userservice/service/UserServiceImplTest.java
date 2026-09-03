package com.ecommerce.userservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
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

import com.ecommerce.userservice.dto.CreateUserRequest;
import com.ecommerce.userservice.dto.UpdateUserRequest;
import com.ecommerce.userservice.dto.UserResponse;
import com.ecommerce.userservice.entity.Role;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.exception.EmailAlreadyExistsException;
import com.ecommerce.userservice.exception.ForbiddenException;
import com.ecommerce.userservice.exception.ResourceNotFoundException;
import com.ecommerce.userservice.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class UserServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private UserServiceImpl userService;

	private User user;
	private CreateUserRequest createRequest;
	private UpdateUserRequest updateRequest;

	@BeforeEach
	void setUp() {

		user = User.builder().id(1L).firstName("John").lastName("Smith").email("john.smith@example.com")
				.password("encodedPassword").role(Role.CUSTOMER).build();

		createRequest = CreateUserRequest.builder().firstName("John").lastName("Smith").email("john.smith@example.com")
				.password("password123").role(Role.CUSTOMER).build();

		updateRequest = UpdateUserRequest.builder().firstName("Michael").lastName("Brown")
				.email("michael.brown@example.com").role(Role.ADMIN).build();
	}

	@Test
	void createUser_shouldCreateUserSuccessfully_whenAdmin() {

		when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(false);
		when(passwordEncoder.encode(createRequest.getPassword())).thenReturn("encodedPassword");
		when(userRepository.save(any(User.class))).thenReturn(user);

		UserResponse response = userService.createUser(createRequest, "ADMIN");

		assertNotNull(response);
		assertEquals(user.getId(), response.getId());
		assertEquals(user.getFirstName(), response.getFirstName());
		assertEquals(user.getLastName(), response.getLastName());
		assertEquals(user.getEmail(), response.getEmail());
		assertEquals(user.getRole(), response.getRole());

		verify(userRepository).existsByEmail(createRequest.getEmail());
		verify(passwordEncoder).encode(createRequest.getPassword());
		verify(userRepository).save(any(User.class));
	}

	@Test
	void createUser_shouldRejectCustomer() {

		assertThrows(ForbiddenException.class, () -> userService.createUser(createRequest, "CUSTOMER"));

		verify(userRepository, never()).existsByEmail(any());
		verify(passwordEncoder, never()).encode(any());
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void createUser_shouldEncodePasswordBeforeSavingUser() {

		when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(false);
		when(passwordEncoder.encode(createRequest.getPassword())).thenReturn("encodedPassword");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		userService.createUser(createRequest, "ADMIN");

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

		verify(userRepository).save(userCaptor.capture());

		User savedUser = userCaptor.getValue();

		assertEquals("encodedPassword", savedUser.getPassword());
		assertEquals(createRequest.getEmail(), savedUser.getEmail());
		assertEquals(createRequest.getFirstName(), savedUser.getFirstName());
		assertEquals(createRequest.getLastName(), savedUser.getLastName());
		assertEquals(createRequest.getRole(), savedUser.getRole());
	}

	@Test
	void createUser_shouldThrowException_whenEmailAlreadyExists() {

		when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(true);

		assertThrows(EmailAlreadyExistsException.class, () -> userService.createUser(createRequest, "ADMIN"));

		verify(userRepository).existsByEmail(createRequest.getEmail());
		verify(passwordEncoder, never()).encode(any());
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void getUserById_shouldAllowUserToAccessOwnData() {

		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		UserResponse response = userService.getUserById(1L, 1L, "CUSTOMER");

		assertNotNull(response);
		assertEquals(user.getId(), response.getId());
		assertEquals(user.getEmail(), response.getEmail());
		assertEquals(user.getRole(), response.getRole());

		verify(userRepository).findById(1L);
	}

	@Test
	void getUserById_shouldAllowAdminToAccessAnyUser() {

		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		UserResponse response = userService.getUserById(1L, 99L, "ADMIN");

		assertNotNull(response);
		assertEquals(1L, response.getId());

		verify(userRepository).findById(1L);
	}

	@Test
	void getUserById_shouldRejectCustomerAccessToAnotherUser() {

		assertThrows(ForbiddenException.class, () -> userService.getUserById(1L, 99L, "CUSTOMER"));

		verify(userRepository, never()).findById(any());
	}

	@Test
	void getUserById_shouldThrowException_whenUserNotFound() {

		when(userRepository.findById(1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(1L, 1L, "CUSTOMER"));

		verify(userRepository).findById(1L);
	}

	@Test
	void getAllUsers_shouldAllowAdmin() {

		User secondUser = User.builder().id(2L).firstName("Emily").lastName("Johnson")
				.email("emily.johnson@example.com").password("encodedPassword").role(Role.CUSTOMER).build();

		when(userRepository.findAll()).thenReturn(List.of(user, secondUser));

		List<UserResponse> response = userService.getAllUsers("ADMIN");

		assertNotNull(response);
		assertEquals(2, response.size());
		assertEquals(user.getId(), response.get(0).getId());
		assertEquals(secondUser.getId(), response.get(1).getId());

		verify(userRepository).findAll();
	}

	@Test
	void getAllUsers_shouldRejectCustomer() {

		assertThrows(ForbiddenException.class, () -> userService.getAllUsers("CUSTOMER"));

		verify(userRepository, never()).findAll();
	}

	@Test
	void getAllUsers_shouldReturnEmptyList_whenNoUsersExist() {

		when(userRepository.findAll()).thenReturn(List.of());

		List<UserResponse> response = userService.getAllUsers("ADMIN");

		assertNotNull(response);
		assertEquals(0, response.size());

		verify(userRepository).findAll();
	}

	@Test
	void getUserByIdInternal_shouldReturnUser() {

		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		UserResponse response = userService.getUserByIdInternal(1L);

		assertNotNull(response);
		assertEquals(user.getId(), response.getId());
		assertEquals(user.getEmail(), response.getEmail());
		assertEquals(user.getRole(), response.getRole());

		verify(userRepository).findById(1L);
	}

	@Test
	void updateUser_shouldAllowUserToUpdateOwnData() {

		UpdateUserRequest customerRequest = UpdateUserRequest.builder().firstName("Michael").lastName("Brown")
				.email("michael.brown@example.com").role(Role.ADMIN).build();

		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userRepository.existsByEmail(customerRequest.getEmail())).thenReturn(false);
		when(userRepository.save(user)).thenReturn(user);

		UserResponse response = userService.updateUser(1L, customerRequest, 1L, "CUSTOMER");

		assertNotNull(response);
		assertEquals("Michael", user.getFirstName());
		assertEquals("Brown", user.getLastName());
		assertEquals("michael.brown@example.com", user.getEmail());

		// CUSTOMER cannot change role.
		assertEquals(Role.CUSTOMER, user.getRole());

		verify(userRepository).findById(1L);
		verify(userRepository).existsByEmail(customerRequest.getEmail());
		verify(userRepository).save(user);
	}

	@Test
	void updateUser_shouldAllowAdminToUpdateAnyUser() {

		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userRepository.existsByEmail(updateRequest.getEmail())).thenReturn(false);
		when(userRepository.save(user)).thenReturn(user);

		UserResponse response = userService.updateUser(1L, updateRequest, 99L, "ADMIN");

		assertNotNull(response);
		assertEquals("Michael", user.getFirstName());
		assertEquals("Brown", user.getLastName());
		assertEquals("michael.brown@example.com", user.getEmail());
		assertEquals(Role.ADMIN, user.getRole());

		verify(userRepository).findById(1L);
		verify(userRepository).existsByEmail(updateRequest.getEmail());
		verify(userRepository).save(user);
	}

	@Test
	void updateUser_shouldRejectCustomerUpdatingAnotherUser() {

		assertThrows(ForbiddenException.class, () -> userService.updateUser(1L, updateRequest, 99L, "CUSTOMER"));

		verify(userRepository, never()).findById(any());
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void updateUser_shouldThrowException_whenNewEmailAlreadyExists() {

		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userRepository.existsByEmail(updateRequest.getEmail())).thenReturn(true);

		assertThrows(EmailAlreadyExistsException.class,
				() -> userService.updateUser(1L, updateRequest, 1L, "CUSTOMER"));

		verify(userRepository).findById(1L);
		verify(userRepository).existsByEmail(updateRequest.getEmail());
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void updateUser_shouldAllowSameEmail() {

		UpdateUserRequest sameEmailRequest = UpdateUserRequest.builder().firstName("Michael").lastName("Brown")
				.email(user.getEmail()).role(Role.ADMIN).build();

		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userRepository.save(user)).thenReturn(user);

		UserResponse response = userService.updateUser(1L, sameEmailRequest, 1L, "CUSTOMER");

		assertNotNull(response);
		assertEquals("Michael", response.getFirstName());
		assertEquals("Brown", response.getLastName());
		assertEquals(user.getEmail(), response.getEmail());

		// CUSTOMER cannot change role.
		assertEquals(Role.CUSTOMER, response.getRole());

		verify(userRepository).findById(1L);
		verify(userRepository, never()).existsByEmail(any());
		verify(userRepository).save(user);
	}

	@Test
	void updateUser_shouldThrowException_whenUserNotFound() {

		when(userRepository.findById(1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> userService.updateUser(1L, updateRequest, 1L, "CUSTOMER"));

		verify(userRepository).findById(1L);
		verify(userRepository, never()).existsByEmail(any());
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void deleteUser_shouldAllowAdmin() {

		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		userService.deleteUser(1L, "ADMIN");

		verify(userRepository).findById(1L);
		verify(userRepository).delete(user);
	}

	@Test
	void deleteUser_shouldRejectCustomer() {

		assertThrows(ForbiddenException.class, () -> userService.deleteUser(1L, "CUSTOMER"));

		verify(userRepository, never()).findById(any());
		verify(userRepository, never()).delete(any(User.class));
	}

	@Test
	void deleteUser_shouldThrowException_whenUserNotFound() {

		when(userRepository.findById(1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(1L, "ADMIN"));

		verify(userRepository).findById(1L);
		verify(userRepository, never()).delete(any(User.class));
	}
}