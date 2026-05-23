package com.att.tdp.issueflow.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.att.tdp.issueflow.model.entity.User;
import com.att.tdp.issueflow.model.enums.Role;
import com.att.tdp.issueflow.repository.RevokedJwtTokenRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import com.att.tdp.issueflow.support.TestPasswords;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RevokedJwtTokenRepository revokedJwtTokenRepository;

	@BeforeEach
	void setUp() {
		revokedJwtTokenRepository.deleteAll();
		userRepository.deleteAll();
		saveUser("admin", Role.ADMIN);
		saveUser("dev", Role.DEVELOPER);
	}

	@Test
	void protectedEndpoint_requiresAuthentication() throws Exception {
		mockMvc.perform(get("/users")).andExpect(status().isUnauthorized());
	}

	@Test
	void login_allowsAccessWithBearerToken() throws Exception {
		String token = login("dev");

		mockMvc.perform(get("/users").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void adminOnlyEndpoint_forbidsDeveloper() throws Exception {
		String token = login("dev");

		mockMvc.perform(get("/projects/deleted").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminOnlyEndpoint_allowsAdmin() throws Exception {
		String token = login("admin");

		mockMvc.perform(get("/projects/deleted").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void login_allowsAccessWithDoubleBearerPrefix() throws Exception {
		String token = login("dev");

		mockMvc.perform(get("/users").header("Authorization", "Bearer Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void login_allowsAccessWithLowercaseBearerPrefix() throws Exception {
		String token = login("dev");

		mockMvc.perform(get("/users").header("Authorization", "bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void logout_revokesToken() throws Exception {
		String token = login("dev");

		mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		mockMvc.perform(get("/users").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void me_returnsCurrentUser() throws Exception {
		String token = login("dev");

		mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("dev"));
	}

	private String login(String username) throws Exception {
		String body =
				"""
				{
				  "username": "%s",
				  "password": "%s"
				}
				"""
						.formatted(username, TestPasswords.RAW);

		String response = mockMvc.perform(
						post("/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(body))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		int start = response.indexOf("\"accessToken\":\"") + 15;
		int end = response.indexOf('"', start);
		return response.substring(start, end);
	}

	private void saveUser(String username, Role role) {
		User user = new User();
		user.setUsername(username);
		user.setEmail(username + "@example.com");
		user.setFullName(username);
		user.setRole(role);
		user.setPasswordHash(TestPasswords.encoded());
		userRepository.save(user);
	}
}
