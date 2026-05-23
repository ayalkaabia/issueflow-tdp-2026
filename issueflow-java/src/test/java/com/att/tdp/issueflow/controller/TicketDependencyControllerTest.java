package com.att.tdp.issueflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.att.tdp.issueflow.controller.support.SecuredControllerTestBase;
import com.att.tdp.issueflow.dto.response.DependencyTicketResponse;
import com.att.tdp.issueflow.model.enums.TicketStatus;
import com.att.tdp.issueflow.service.TicketDependencyService;
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
@WebMvcTest(value = TicketDependencyController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class TicketDependencyControllerTest extends SecuredControllerTestBase {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TicketDependencyService ticketDependencyService;

	@Test
	void listDependencies_returnsBlockers() throws Exception {
		DependencyTicketResponse blocker = DependencyTicketResponse.builder()
				.id(42L)
				.title("Blocking ticket")
				.status(TicketStatus.IN_PROGRESS)
				.build();
		when(ticketDependencyService.listDependencies(1L)).thenReturn(List.of(blocker));

		mockMvc.perform(get("/tickets/1/dependencies"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(42))
				.andExpect(jsonPath("$[0].title").value("Blocking ticket"))
				.andExpect(jsonPath("$[0].status").value("IN_PROGRESS"));
	}

	@Test
	void addDependency_returnsOk() throws Exception {
		mockMvc.perform(
						post("/tickets/1/dependencies")
								.contentType(MediaType.APPLICATION_JSON)
								.content("{ \"blockedBy\": 42 }"))
				.andExpect(status().isOk());

		verify(ticketDependencyService).addDependency(eq(1L), any(), eq(1L));
	}

	@Test
	void removeDependency_returnsOk() throws Exception {
		mockMvc.perform(delete("/tickets/1/dependencies/42")).andExpect(status().isOk());

		verify(ticketDependencyService).removeDependency(1L, 42L, 1L);
	}
}
