package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.response.AuditLogResponse;
import com.att.tdp.issueflow.mapper.AuditLogMapper;
import com.att.tdp.issueflow.model.entity.AuditLog;
import com.att.tdp.issueflow.model.enums.AuditAction;
import com.att.tdp.issueflow.model.enums.AuditActor;
import com.att.tdp.issueflow.model.enums.AuditEntityType;
import com.att.tdp.issueflow.repository.AuditLogRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

	private final AuditLogRepository auditLogRepository;

	@Transactional(readOnly = true)
	public List<AuditLogResponse> getAuditLogs(
			AuditEntityType entityType, Long entityId, AuditAction action, AuditActor actor) {
		Specification<AuditLog> specification = Specification.where(null);

		if (entityType != null) {
			specification = specification.and(
					(root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("entityType"), entityType));
		}
		if (entityId != null) {
			specification = specification.and(
					(root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("entityId"), entityId));
		}
		if (action != null) {
			specification = specification.and(
					(root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("action"), action));
		}
		if (actor != null) {
			specification = specification.and(
					(root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("actor"), actor));
		}

		return auditLogRepository
				.findAll(specification, Sort.by(Sort.Direction.DESC, "timestamp"))
				.stream()
				.map(AuditLogMapper::toResponse)
				.toList();
	}
}
