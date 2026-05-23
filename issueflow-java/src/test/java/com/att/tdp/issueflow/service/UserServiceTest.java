package com.att.tdp.issueflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.att.tdp.issueflow.dto.request.CreateUserRequest;
import com.att.tdp.issueflow.dto.request.UpdateUserRequest;
import com.att.tdp.issueflow.exception.BusinessRuleException;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.model.enums.Role;
import com.att.tdp.issueflow.repository.UserRepository;
import com.att.tdp.issueflow.support.TestPasswords;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserServiceTest {

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();
	}

	@Test
	void createUser_andGetById() {
		var created = userService.createUser(createRequest("jdoe", "jdoe@example.com"), null);

		var fetched = userService.getUserById(created.getId());

		assertThat(fetched.getUsername()).isEqualTo("jdoe");
		assertThat(fetched.getRole()).isEqualTo(Role.DEVELOPER);
	}

	@Test
	void createUser_rejectsDuplicateUsername() {
		userService.createUser(createRequest("jdoe", "jdoe@example.com"), null);

		var duplicate = createRequest("jdoe", "other@example.com");

		assertThatThrownBy(() -> userService.createUser(duplicate, null))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Username already exists");
	}

	@Test
	void updateUser_changesRole() {
		var created = userService.createUser(createRequest("jdoe", "jdoe@example.com"), null);

		UpdateUserRequest update = new UpdateUserRequest();
		update.setRole(Role.ADMIN);

		userService.updateUser(created.getId(), update, null);

		assertThat(userService.getUserById(created.getId()).getRole()).isEqualTo(Role.ADMIN);
	}

	@Test
	void deleteUser_removesUser() {
		var created = userService.createUser(createRequest("jdoe", "jdoe@example.com"), null);

		userService.deleteUser(created.getId(), null);

		assertThat(userRepository.findById(created.getId())).isEmpty();
	}

	@Test
	void getUserById_rejectsMissing() {
		assertThatThrownBy(() -> userService.getUserById(999L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	private CreateUserRequest createRequest(String username, String email) {
		CreateUserRequest request = new CreateUserRequest();
		request.setUsername(username);
		request.setEmail(email);
		request.setFullName("John Doe");
		request.setRole(Role.DEVELOPER);
		request.setPassword(TestPasswords.RAW);
		return request;
	}
}
