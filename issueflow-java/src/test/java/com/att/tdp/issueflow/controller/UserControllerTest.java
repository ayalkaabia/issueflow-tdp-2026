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

import com.att.tdp.issueflow.controller.support.SecuredControllerTestBase;
import com.att.tdp.issueflow.dto.response.CommentResponse;
import com.att.tdp.issueflow.dto.response.MentionPageResponse;
import com.att.tdp.issueflow.dto.response.MentionedUserResponse;
import com.att.tdp.issueflow.dto.response.UserResponse;
import com.att.tdp.issueflow.model.enums.Role;
import com.att.tdp.issueflow.service.MentionService;
import com.att.tdp.issueflow.service.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = UserController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest extends SecuredControllerTestBase {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private MentionService mentionService;

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
		when(userService.createUser(any(), eq(1L))).thenReturn(sampleUser());

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

		verify(userService).createUser(any(), eq(1L));
	}

	@Test
	void getMentionsForUser_returnsPagedComments() throws Exception {
		MentionPageResponse page = MentionPageResponse.builder()
				.data(List.of(CommentResponse.builder()
						.id(1L)
						.ticketId(3L)
						.authorId(2L)
						.content("Hey @jdoe")
						.version(0L)
						.mentionedUsers(List.of(MentionedUserResponse.builder()
								.id(1L)
								.username("jdoe")
								.fullName("John Doe")
								.build()))
						.build()))
				.total(10)
				.page(1)
				.build();
		when(mentionService.getMentionsForUser(1L, 1, 10)).thenReturn(page);

		mockMvc.perform(get("/users/1/mentions?page=1&pageSize=10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(10))
				.andExpect(jsonPath("$.page").value(1))
				.andExpect(jsonPath("$.data[0].mentionedUsers[0].username").value("jdoe"));

		verify(mentionService).getMentionsForUser(1L, 1, 10);
	}

	@Test
	void updateUser_usesReadmePathAndEmptyBody() throws Exception {
		doNothing().when(userService).updateUser(eq(1L), any(), eq(1L));

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

		verify(userService).updateUser(eq(1L), any(), eq(1L));
	}

	@Test
	void deleteUser_returnsOk() throws Exception {
		doNothing().when(userService).deleteUser(eq(1L), eq(1L));

		mockMvc.perform(delete("/users/1"))
				.andExpect(status().isOk())
				.andExpect(content().string(""));

		verify(userService).deleteUser(eq(1L), eq(1L));
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
