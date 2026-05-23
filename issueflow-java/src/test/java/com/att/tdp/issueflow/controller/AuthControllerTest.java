package com.att.tdp.issueflow.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.att.tdp.issueflow.dto.response.LoginResponse;
import com.att.tdp.issueflow.dto.response.UserResponse;
import com.att.tdp.issueflow.model.enums.Role;
import com.att.tdp.issueflow.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = AuthController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@Test
	void login_returnsTokenPayload() throws Exception {
		when(authService.login(org.mockito.ArgumentMatchers.any()))
				.thenReturn(LoginResponse.builder()
						.accessToken("token-123")
						.tokenType("Bearer")
						.expiresIn(3600)
						.build());

		mockMvc.perform(
						post("/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "username": "jdoe",
										  "password": "secret"
										}
										"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("token-123"))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(3600));
	}

	@Test
	void me_returnsCurrentUser() throws Exception {
		when(authService.getCurrentUser())
				.thenReturn(UserResponse.builder()
						.id(1L)
						.username("jdoe")
						.email("jdoe@example.com")
						.fullName("John Doe")
						.role(Role.DEVELOPER)
						.build());

		mockMvc.perform(get("/auth/me")).andExpect(status().isOk()).andExpect(jsonPath("$.username").value("jdoe"));
	}

	@Test
	void logout_delegatesToService() throws Exception {
		mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer token-123"))
				.andExpect(status().isOk());

		verify(authService).logout("Bearer token-123");
	}
}
