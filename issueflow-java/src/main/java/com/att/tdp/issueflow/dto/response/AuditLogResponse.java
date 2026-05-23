package com.att.tdp.issueflow.dto.response;

import com.att.tdp.issueflow.model.enums.AuditAction;
import com.att.tdp.issueflow.model.enums.AuditActor;
import com.att.tdp.issueflow.model.enums.AuditEntityType;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuditLogResponse {

	private Long id;
	private AuditAction action;
	private AuditEntityType entityType;
	private Long entityId;
	private Long performedBy;
	private AuditActor actor;
	private Instant timestamp;
}
