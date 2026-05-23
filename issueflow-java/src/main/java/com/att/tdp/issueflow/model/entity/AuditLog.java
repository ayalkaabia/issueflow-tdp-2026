package com.att.tdp.issueflow.model.entity;

import com.att.tdp.issueflow.model.enums.AuditAction;
import com.att.tdp.issueflow.model.enums.AuditActor;
import com.att.tdp.issueflow.model.enums.AuditEntityType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AuditAction action;

	@Enumerated(EnumType.STRING)
	@Column(name = "entity_type", nullable = false)
	private AuditEntityType entityType;

	@Column(name = "entity_id", nullable = false)
	private Long entityId;

	@Column(name = "performed_by")
	private Long performedBy;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AuditActor actor;

	@Column(nullable = false)
	private Instant timestamp;

	@PrePersist
	void onCreate() {
		if (timestamp == null) {
			timestamp = Instant.now();
		}
	}
}
