package com.att.tdp.issueflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.att.tdp.issueflow.controller.support.SecuredControllerTestBase;
import com.att.tdp.issueflow.dto.response.TicketResponse;
import com.att.tdp.issueflow.model.enums.TicketPriority;
import com.att.tdp.issueflow.model.enums.TicketStatus;
import com.att.tdp.issueflow.model.enums.TicketType;
import com.att.tdp.issueflow.service.TicketCsvService;
import com.att.tdp.issueflow.service.TicketService;
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
@WebMvcTest(value = TicketController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class TicketControllerTest extends SecuredControllerTestBase {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TicketService ticketService;

	@MockitoBean
	private TicketCsvService ticketCsvService;

	@Test
	void exportTickets_returnsCsv() throws Exception {
		when(ticketCsvService.exportTickets(1L)).thenReturn("id,title\n1,Task".getBytes());

		mockMvc.perform(get("/tickets/export").param("projectId", "1"))
				.andExpect(status().isOk())
				.andExpect(
						org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
								.string("Content-Disposition", "attachment; filename=\"tickets-1.csv\""));

		verify(ticketCsvService).exportTickets(1L);
	}

	@Test
	void getTicketsByProject_returnsList() throws Exception {
		TicketResponse ticket = sampleResponse();
		when(ticketService.getTicketsByProject(1L)).thenReturn(List.of(ticket));

		mockMvc.perform(get("/tickets").param("projectId", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].isOverdue").value(false));

		verify(ticketService).getTicketsByProject(1L);
	}

	@Test
	void getTicketById_returnsTicket() throws Exception {
		when(ticketService.getTicketById(1L)).thenReturn(sampleResponse());

		mockMvc.perform(get("/tickets/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));

		verify(ticketService).getTicketById(1L);
	}

	@Test
	void createTicket_returnsCreatedTicket() throws Exception {
		when(ticketService.createTicket(any(), eq(1L))).thenReturn(sampleResponse());

		mockMvc.perform(
						post("/tickets")
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "title": "Fix login bug",
										  "priority": "HIGH",
										  "type": "BUG",
										  "projectId": 1
										}
										"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Fix login bug"))
				.andExpect(jsonPath("$.status").value("TODO"));

		verify(ticketService).createTicket(any(), eq(1L));
	}

	@Test
	void updateTicket_requiresVersion() throws Exception {
		mockMvc.perform(
						patch("/tickets/1")
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "status": "IN_PROGRESS"
										}
										"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void getDeletedTickets_usesCorrectPath() throws Exception {
		when(ticketService.getDeletedTickets(1L, 1L)).thenReturn(List.of(sampleResponse()));

		mockMvc.perform(get("/tickets/deleted").param("projectId", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1));

		verify(ticketService).getDeletedTickets(1L, 1L);
	}

	@Test
	void restoreTicket_usesCorrectPath() throws Exception {
		when(ticketService.restoreTicket(1L, 1L)).thenReturn(sampleResponse());

		mockMvc.perform(post("/tickets/1/restore")).andExpect(status().isOk());

		verify(ticketService).restoreTicket(1L, 1L);
	}

	@Test
	void softDeleteTicket_returnsOk() throws Exception {
		doNothing().when(ticketService).softDeleteTicket(1L, 1L);

		mockMvc.perform(delete("/tickets/1")).andExpect(status().isOk());

		verify(ticketService).softDeleteTicket(1L, 1L);
	}

	@Test
	void updateTicket_acceptsPatchWithVersion() throws Exception {
		when(ticketService.updateTicket(eq(1L), any(), eq(1L))).thenReturn(sampleResponse());

		mockMvc.perform(
						patch("/tickets/1")
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "status": "IN_PROGRESS",
										  "version": 0
										}
										"""))
				.andExpect(status().isOk());

		verify(ticketService).updateTicket(eq(1L), any(), eq(1L));
	}

	private TicketResponse sampleResponse() {
		return TicketResponse.builder()
				.id(1L)
				.title("Fix login bug")
				.description("...")
				.status(TicketStatus.TODO)
				.priority(TicketPriority.HIGH)
				.type(TicketType.BUG)
				.projectId(1L)
				.assigneeId(2L)
				.overdue(false)
				.version(0L)
				.build();
	}
}
