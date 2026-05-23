package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.response.AttachmentResponse;
import com.att.tdp.issueflow.security.SecurityUtils;
import com.att.tdp.issueflow.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/tickets/{ticketId}/attachments")
@RequiredArgsConstructor
public class AttachmentController {

	private final AttachmentService attachmentService;

	@PostMapping
	public AttachmentResponse uploadAttachment(
			@PathVariable Long ticketId, @RequestParam("file") MultipartFile file) {
		return attachmentService.uploadAttachment(ticketId, file, SecurityUtils.getCurrentUserId());
	}

	@DeleteMapping("/{attachmentId}")
	public ResponseEntity<Void> deleteAttachment(
			@PathVariable Long ticketId, @PathVariable Long attachmentId) {
		attachmentService.deleteAttachment(ticketId, attachmentId, SecurityUtils.getCurrentUserId());
		return ResponseEntity.ok().build();
	}
}
