package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.request.CreateTicketRequest;
import com.att.tdp.issueflow.dto.request.UpdateTicketRequest;
import com.att.tdp.issueflow.dto.response.TicketImportResponse;
import com.att.tdp.issueflow.dto.response.TicketResponse;
import com.att.tdp.issueflow.security.SecurityUtils;
import com.att.tdp.issueflow.service.TicketCsvService;
import com.att.tdp.issueflow.service.TicketService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/tickets")
@Validated
@RequiredArgsConstructor
public class TicketController {

	private final TicketService ticketService;
	private final TicketCsvService ticketCsvService;

	@GetMapping("/export")
	public ResponseEntity<byte[]> exportTickets(@RequestParam @NotNull Long projectId) {
		byte[] csv = ticketCsvService.exportTickets(projectId);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tickets-" + projectId + ".csv\"")
				.contentType(MediaType.parseMediaType("text/csv"))
				.body(csv);
	}

	@PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public TicketImportResponse importTickets(
			@RequestParam("file") MultipartFile file, @RequestParam @NotNull Long projectId) {
		return ticketCsvService.importTickets(projectId, file, SecurityUtils.getCurrentUserId());
	}

	@GetMapping
	public List<TicketResponse> getTicketsByProject(@RequestParam @NotNull Long projectId) {
		return ticketService.getTicketsByProject(projectId);
	}

	@GetMapping("/deleted")
	public List<TicketResponse> getDeletedTickets(@RequestParam @NotNull Long projectId) {
		return ticketService.getDeletedTickets(projectId, SecurityUtils.getCurrentUserId());
	}

	@GetMapping("/{ticketId}")
	public TicketResponse getTicketById(@PathVariable Long ticketId) {
		return ticketService.getTicketById(ticketId);
	}

	@PostMapping
	public TicketResponse createTicket(@Valid @RequestBody CreateTicketRequest request) {
		return ticketService.createTicket(request, SecurityUtils.getCurrentUserId());
	}

	@PatchMapping("/{ticketId}")
	public TicketResponse updateTicket(
			@PathVariable Long ticketId, @Valid @RequestBody UpdateTicketRequest request) {
		return ticketService.updateTicket(ticketId, request, SecurityUtils.getCurrentUserId());
	}

	@DeleteMapping("/{ticketId}")
	public ResponseEntity<Void> softDeleteTicket(@PathVariable Long ticketId) {
		ticketService.softDeleteTicket(ticketId, SecurityUtils.getCurrentUserId());
		return ResponseEntity.ok().build();
	}

	@PostMapping("/{ticketId}/restore")
	public TicketResponse restoreTicket(@PathVariable Long ticketId) {
		return ticketService.restoreTicket(ticketId, SecurityUtils.getCurrentUserId());
	}
}
