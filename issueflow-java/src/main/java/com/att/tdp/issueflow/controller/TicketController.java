package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.request.CreateTicketRequest;
import com.att.tdp.issueflow.dto.request.UpdateTicketRequest;
import com.att.tdp.issueflow.dto.response.TicketResponse;
import com.att.tdp.issueflow.security.SecurityUtils;
import com.att.tdp.issueflow.service.TicketService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tickets")
@Validated
@RequiredArgsConstructor
public class TicketController {

	private final TicketService ticketService;

	@GetMapping
	public List<TicketResponse> getTicketsByProject(@RequestParam @NotNull Long projectId) {
		return ticketService.getTicketsByProject(projectId);
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
}
