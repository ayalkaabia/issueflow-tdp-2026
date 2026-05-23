package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.model.entity.AuditLog;
import com.att.tdp.issueflow.model.enums.AuditAction;
import com.att.tdp.issueflow.model.enums.AuditActor;
import com.att.tdp.issueflow.model.enums.AuditEntityType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

	List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(
			AuditEntityType entityType, Long entityId);

	List<AuditLog> findByEntityTypeOrderByTimestampDesc(AuditEntityType entityType);

	List<AuditLog> findByActionOrderByTimestampDesc(AuditAction action);

	List<AuditLog> findByActorOrderByTimestampDesc(AuditActor actor);

	List<AuditLog> findAllByOrderByTimestampDesc();
}
