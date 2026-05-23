package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.request.AddDependencyRequest;
import com.att.tdp.issueflow.dto.response.DependencyTicketResponse;
import com.att.tdp.issueflow.security.SecurityUtils;
import com.att.tdp.issueflow.service.TicketDependencyService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tickets/{ticketId}/dependencies")
@Validated
@RequiredArgsConstructor
public class TicketDependencyController {

	private final TicketDependencyService ticketDependencyService;

	@PostMapping
	public ResponseEntity<Void> addDependency(
			@PathVariable Long ticketId, @Valid @RequestBody AddDependencyRequest request) {
		ticketDependencyService.addDependency(ticketId, request, SecurityUtils.getCurrentUserId());
		return ResponseEntity.ok().build();
	}

	@GetMapping
	public List<DependencyTicketResponse> listDependencies(@PathVariable Long ticketId) {
		return ticketDependencyService.listDependencies(ticketId);
	}

	@DeleteMapping("/{blockerId}")
	public ResponseEntity<Void> removeDependency(
			@PathVariable Long ticketId, @PathVariable Long blockerId) {
		ticketDependencyService.removeDependency(ticketId, blockerId, SecurityUtils.getCurrentUserId());
		return ResponseEntity.ok().build();
	}
}
