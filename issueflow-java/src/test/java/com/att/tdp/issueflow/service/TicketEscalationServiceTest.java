package com.att.tdp.issueflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.tdp.issueflow.model.entity.Project;
import com.att.tdp.issueflow.model.entity.Ticket;
import com.att.tdp.issueflow.model.entity.User;
import com.att.tdp.issueflow.model.enums.AuditAction;
import com.att.tdp.issueflow.model.enums.AuditActor;
import com.att.tdp.issueflow.model.enums.AuditEntityType;
import com.att.tdp.issueflow.model.enums.Role;
import com.att.tdp.issueflow.model.enums.TicketPriority;
import com.att.tdp.issueflow.model.enums.TicketStatus;
import com.att.tdp.issueflow.model.enums.TicketType;
import com.att.tdp.issueflow.repository.AuditLogRepository;
import com.att.tdp.issueflow.repository.ProjectRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import com.att.tdp.issueflow.support.TestPasswords;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TicketEscalationServiceTest {

	@Autowired
	private TicketEscalationService ticketEscalationService;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AuditLogRepository auditLogRepository;

	private Long projectId;

	@BeforeEach
	void setUp() {
		auditLogRepository.deleteAll();
		ticketRepository.deleteAll();
		projectRepository.deleteAll();
		userRepository.deleteAll();

		User owner = new User();
		owner.setUsername("owner");
		owner.setEmail("owner@example.com");
		owner.setFullName("Owner");
		owner.setRole(Role.ADMIN);
		owner.setPasswordHash(TestPasswords.encoded());
		Long ownerId = userRepository.save(owner).getId();

		Project project = new Project();
		project.setName("Main");
		project.setOwnerId(ownerId);
		projectId = projectRepository.save(project).getId();
	}

	@Test
	void escalateOverdueTickets_raisesPriorityAndWritesAudit() {
		Ticket ticket = saveOverdueTicket(TicketPriority.MEDIUM, TicketStatus.TODO);
		auditLogRepository.deleteAll();

		ticketEscalationService.escalateOverdueTickets();

		Ticket updated = ticketRepository.findById(ticket.getId()).orElseThrow();
		assertThat(updated.getPriority()).isEqualTo(TicketPriority.HIGH);
		assertThat(updated.isOverdue()).isTrue();

		var logs = auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(
				AuditEntityType.TICKET, ticket.getId());
		assertThat(logs).hasSize(1);
		assertThat(logs.get(0).getAction()).isEqualTo(AuditAction.ESCALATE);
		assertThat(logs.get(0).getActor()).isEqualTo(AuditActor.SYSTEM);
	}

	@Test
	void escalateOverdueTickets_skipsDoneTickets() {
		Ticket ticket = saveOverdueTicket(TicketPriority.LOW, TicketStatus.DONE);
		auditLogRepository.deleteAll();

		ticketEscalationService.escalateOverdueTickets();

		Ticket updated = ticketRepository.findById(ticket.getId()).orElseThrow();
		assertThat(updated.getPriority()).isEqualTo(TicketPriority.LOW);

		var logs = auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(
				AuditEntityType.TICKET, ticket.getId());
		assertThat(logs).isEmpty();
	}

	@Test
	void escalateOverdueTickets_doesNotEscalateBeyondCritical() {
		Ticket ticket = saveOverdueTicket(TicketPriority.CRITICAL, TicketStatus.IN_PROGRESS);
		auditLogRepository.deleteAll();

		ticketEscalationService.escalateOverdueTickets();

		Ticket updated = ticketRepository.findById(ticket.getId()).orElseThrow();
		assertThat(updated.getPriority()).isEqualTo(TicketPriority.CRITICAL);
		assertThat(updated.isOverdue()).isTrue();

		var logs = auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(
				AuditEntityType.TICKET, ticket.getId());
		assertThat(logs).isEmpty();
	}

	@Test
	void escalatePriority_stepsThroughLevels() {
		assertThat(TicketEscalationService.escalatePriority(TicketPriority.LOW))
				.isEqualTo(TicketPriority.MEDIUM);
		assertThat(TicketEscalationService.escalatePriority(TicketPriority.MEDIUM))
				.isEqualTo(TicketPriority.HIGH);
		assertThat(TicketEscalationService.escalatePriority(TicketPriority.HIGH))
				.isEqualTo(TicketPriority.CRITICAL);
		assertThat(TicketEscalationService.escalatePriority(TicketPriority.CRITICAL))
				.isEqualTo(TicketPriority.CRITICAL);
	}

	private Ticket saveOverdueTicket(TicketPriority priority, TicketStatus status) {
		Ticket ticket = new Ticket();
		ticket.setTitle("Overdue task");
		ticket.setPriority(priority);
		ticket.setType(TicketType.TECHNICAL);
		ticket.setStatus(status);
		ticket.setProjectId(projectId);
		ticket.setDueDate(Instant.parse("2020-01-01T00:00:00Z"));
		ticket.setOverdue(false);
		return ticketRepository.save(ticket);
	}
}
