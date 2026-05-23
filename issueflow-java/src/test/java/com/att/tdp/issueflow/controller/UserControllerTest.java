package com.att.tdp.issueflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.att.tdp.issueflow.dto.response.UserResponse;
import com.att.tdp.issueflow.model.enums.Role;
import com.att.tdp.issueflow.service.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@Test
	void getAllUsers_returnsList() throws Exception {
		when(userService.getAllUsers()).thenReturn(List.of(sampleUser()));

		mockMvc.perform(get("/users"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].username").value("jdoe"))
				.andExpect(jsonPath("$[0].role").value("DEVELOPER"));
	}

	@Test
	void createUser_returnsCreatedUser() throws Exception {
		when(userService.createUser(any(), eq(null))).thenReturn(sampleUser());

		mockMvc.perform(
						post("/users")
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "username": "jdoe",
										  "email": "jdoe@example.com",
										  "fullName": "John Doe",
										  "role": "DEVELOPER",
										  "password": "secret"
										}
										"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));

		verify(userService).createUser(any(), eq(null));
	}

	@Test
	void updateUser_usesReadmePathAndEmptyBody() throws Exception {
		doNothing().when(userService).updateUser(eq(1L), any(), eq(null));

		mockMvc.perform(
						post("/users/update/1")
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "fullName": "Jane Doe",
										  "role": "ADMIN"
										}
										"""))
				.andExpect(status().isOk())
				.andExpect(content().string(""));

		verify(userService).updateUser(eq(1L), any(), eq(null));
	}

	@Test
	void deleteUser_returnsOk() throws Exception {
		doNothing().when(userService).deleteUser(eq(1L), eq(null));

		mockMvc.perform(delete("/users/1"))
				.andExpect(status().isOk())
				.andExpect(content().string(""));

		verify(userService).deleteUser(eq(1L), eq(null));
	}

	private UserResponse sampleUser() {
		return UserResponse.builder()
				.id(1L)
				.username("jdoe")
				.email("jdoe@example.com")
				.fullName("John Doe")
				.role(Role.DEVELOPER)
				.build();
	}
}
