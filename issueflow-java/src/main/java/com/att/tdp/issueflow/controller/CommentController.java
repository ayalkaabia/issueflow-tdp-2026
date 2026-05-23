package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.request.CreateCommentRequest;
import com.att.tdp.issueflow.dto.request.UpdateCommentRequest;
import com.att.tdp.issueflow.dto.response.CommentResponse;
import com.att.tdp.issueflow.security.SecurityUtils;
import com.att.tdp.issueflow.service.CommentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tickets/{ticketId}/comments")
@Validated
@RequiredArgsConstructor
public class CommentController {

	private final CommentService commentService;

	@GetMapping
	public List<CommentResponse> getComments(@PathVariable Long ticketId) {
		return commentService.getCommentsByTicket(ticketId);
	}

	@PostMapping
	public CommentResponse addComment(
			@PathVariable Long ticketId, @Valid @RequestBody CreateCommentRequest request) {
		return commentService.addComment(ticketId, request, SecurityUtils.getCurrentUserId());
	}

	@PatchMapping("/{commentId}")
	public CommentResponse updateComment(
			@PathVariable Long ticketId,
			@PathVariable Long commentId,
			@Valid @RequestBody UpdateCommentRequest request) {
		return commentService.updateComment(ticketId, commentId, request, SecurityUtils.getCurrentUserId());
	}

	@DeleteMapping("/{commentId}")
	public ResponseEntity<Void> deleteComment(
			@PathVariable Long ticketId, @PathVariable Long commentId) {
		commentService.deleteComment(ticketId, commentId, SecurityUtils.getCurrentUserId());
		return ResponseEntity.ok().build();
	}
}
