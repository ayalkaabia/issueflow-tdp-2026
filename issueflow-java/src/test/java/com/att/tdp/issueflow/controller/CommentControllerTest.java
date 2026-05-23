package com.att.tdp.issueflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.att.tdp.issueflow.controller.support.SecuredControllerTestBase;
import com.att.tdp.issueflow.dto.response.CommentResponse;
import com.att.tdp.issueflow.dto.response.MentionedUserResponse;
import com.att.tdp.issueflow.service.CommentService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(value = CommentController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class CommentControllerTest extends SecuredControllerTestBase {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CommentService commentService;

	@Test
	void getComments_returnsList() throws Exception {
		CommentResponse comment = sampleResponse();
		when(commentService.getCommentsByTicket(1L)).thenReturn(List.of(comment));

		mockMvc.perform(get("/tickets/1/comments"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].content").value("Hello"));
	}

	@Test
	void addComment_returnsCreatedComment() throws Exception {
		when(commentService.addComment(eq(1L), any(), eq(1L))).thenReturn(sampleResponse());

		mockMvc.perform(
						post("/tickets/1/comments")
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "authorId": 2,
										  "content": "Hello @jdoe!"
										}
										"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.authorId").value(2))
				.andExpect(jsonPath("$.ticketId").value(1));

		verify(commentService).addComment(eq(1L), any(), eq(1L));
	}

	@Test
	void updateComment_requiresVersion() throws Exception {
		mockMvc.perform(
						patch("/tickets/1/comments/5")
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "content": "Updated comment."
										}
										"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void deleteComment_returnsOk() throws Exception {
		mockMvc.perform(delete("/tickets/1/comments/5")).andExpect(status().isOk());

		verify(commentService).deleteComment(1L, 5L, 1L);
	}

	private CommentResponse sampleResponse() {
		return CommentResponse.builder()
				.id(1L)
				.ticketId(1L)
				.authorId(2L)
				.content("Hello")
				.version(0L)
				.mentionedUsers(List.of(MentionedUserResponse.builder()
						.id(1L)
						.username("jdoe")
						.fullName("John Doe")
						.build()))
				.build();
	}
}
