package com.att.tdp.issueflow.mapper;

import com.att.tdp.issueflow.dto.response.AuditLogResponse;
import com.att.tdp.issueflow.model.entity.AuditLog;

public final class AuditLogMapper {

	private AuditLogMapper() {}

	public static AuditLogResponse toResponse(AuditLog auditLog) {
		return AuditLogResponse.builder()
				.id(auditLog.getId())
				.action(auditLog.getAction())
				.entityType(auditLog.getEntityType())
				.entityId(auditLog.getEntityId())
				.performedBy(auditLog.getPerformedBy())
				.actor(auditLog.getActor())
				.timestamp(auditLog.getTimestamp())
				.build();
	}
}
