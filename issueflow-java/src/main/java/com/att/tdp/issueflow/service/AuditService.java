package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.model.enums.AuditAction;
import com.att.tdp.issueflow.model.enums.AuditActor;
import com.att.tdp.issueflow.model.enums.AuditEntityType;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

	/**
	 * Persists audit entries once {@code AuditLog} entity and repository exist (commit 13).
	 */
	public void log(
			AuditAction action,
			AuditEntityType entityType,
			Long entityId,
			Long performedBy,
			AuditActor actor) {
		// no-op until audit persistence is wired
	}
}
