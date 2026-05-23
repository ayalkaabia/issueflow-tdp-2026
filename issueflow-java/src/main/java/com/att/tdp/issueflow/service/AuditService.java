package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.model.entity.AuditLog;
import com.att.tdp.issueflow.model.enums.AuditAction;
import com.att.tdp.issueflow.model.enums.AuditActor;
import com.att.tdp.issueflow.model.enums.AuditEntityType;
import com.att.tdp.issueflow.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

	private final AuditLogRepository auditLogRepository;

	@Transactional
	public void log(
			AuditAction action,
			AuditEntityType entityType,
			Long entityId,
			Long performedBy,
			AuditActor actor) {
		AuditLog auditLog = new AuditLog();
		auditLog.setAction(action);
		auditLog.setEntityType(entityType);
		auditLog.setEntityId(entityId);
		auditLog.setPerformedBy(performedBy);
		auditLog.setActor(actor);
		auditLogRepository.save(auditLog);
	}
}
