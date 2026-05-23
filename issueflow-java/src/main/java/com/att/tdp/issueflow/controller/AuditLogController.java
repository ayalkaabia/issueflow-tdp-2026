package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.response.AuditLogResponse;
import com.att.tdp.issueflow.model.enums.AuditAction;
import com.att.tdp.issueflow.model.enums.AuditActor;
import com.att.tdp.issueflow.model.enums.AuditEntityType;
import com.att.tdp.issueflow.service.AuditLogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

	private final AuditLogService auditLogService;

	@GetMapping
	public List<AuditLogResponse> getAuditLogs(
			@RequestParam(required = false) AuditEntityType entityType,
			@RequestParam(required = false) Long entityId,
			@RequestParam(required = false) AuditAction action,
			@RequestParam(required = false) AuditActor actor) {
		return auditLogService.getAuditLogs(entityType, entityId, action, actor);
	}
}
