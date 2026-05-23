package com.att.tdp.issueflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.att.tdp.issueflow.controller.support.SecuredControllerTestBase;
import com.att.tdp.issueflow.dto.response.AttachmentResponse;
import com.att.tdp.issueflow.service.AttachmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(value = AttachmentController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class AttachmentControllerTest extends SecuredControllerTestBase {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AttachmentService attachmentService;

	@Test
	void uploadAttachment_returnsMetadata() throws Exception {
		AttachmentResponse response = AttachmentResponse.builder()
				.id(1L)
				.ticketId(5L)
				.filename("screenshot.png")
				.contentType("image/png")
				.build();
		when(attachmentService.uploadAttachment(eq(5L), any(), eq(1L))).thenReturn(response);

		mockMvc.perform(
						multipart("/tickets/5/attachments")
								.file(new MockMultipartFile(
										"file", "screenshot.png", "image/png", "bytes".getBytes())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.filename").value("screenshot.png"))
				.andExpect(jsonPath("$.contentType").value("image/png"));

		verify(attachmentService).uploadAttachment(eq(5L), any(), eq(1L));
	}

	@Test
	void deleteAttachment_returnsOk() throws Exception {
		mockMvc.perform(delete("/tickets/5/attachments/1")).andExpect(status().isOk());

		verify(attachmentService).deleteAttachment(5L, 1L, 1L);
	}
}
