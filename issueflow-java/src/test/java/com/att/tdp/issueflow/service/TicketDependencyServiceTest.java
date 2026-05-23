package com.att.tdp.issueflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.att.tdp.issueflow.dto.request.AddDependencyRequest;
import com.att.tdp.issueflow.exception.BusinessRuleException;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.model.entity.Project;
import com.att.tdp.issueflow.model.entity.Ticket;
import com.att.tdp.issueflow.model.entity.User;
import com.att.tdp.issueflow.model.enums.AuditAction;
import com.att.tdp.issueflow.model.enums.AuditEntityType;
import com.att.tdp.issueflow.model.enums.Role;
import com.att.tdp.issueflow.model.enums.TicketPriority;
import com.att.tdp.issueflow.model.enums.TicketStatus;
import com.att.tdp.issueflow.model.enums.TicketType;
import com.att.tdp.issueflow.repository.AuditLogRepository;
import com.att.tdp.issueflow.repository.ProjectRepository;
import com.att.tdp.issueflow.repository.TicketDependencyRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import com.att.tdp.issueflow.support.TestPasswords;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TicketDependencyServiceTest {

	@Autowired
	private TicketDependencyService ticketDependencyService;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private TicketDependencyRepository ticketDependencyRepository;

	@Autowired
	private AuditLogRepository auditLogRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProjectRepository projectRepository;

	private Long projectId;
	private Long otherProjectId;
	private Long ticketId;
	private Long blockerId;
	private Long otherProjectTicketId;

	@BeforeEach
	void setUp() {
		auditLogRepository.deleteAll();
		ticketDependencyRepository.deleteAll();
		ticketRepository.deleteAll();
		projectRepository.deleteAll();
		userRepository.deleteAll();

		Long ownerId = saveUser("owner", Role.ADMIN);

		Project project = new Project();
		project.setName("Main");
		project.setOwnerId(ownerId);
		projectId = projectRepository.save(project).getId();

		Project other = new Project();
		other.setName("Other");
		other.setOwnerId(ownerId);
		otherProjectId = projectRepository.save(other).getId();

		ticketId = saveTicket(projectId, "Ticket A");
		blockerId = saveTicket(projectId, "Blocker");
		otherProjectTicketId = saveTicket(otherProjectId, "Other ticket");
	}

	@Test
	void addDependency_listsBlocker_andWritesAudit() {
		AddDependencyRequest request = new AddDependencyRequest();
		request.setBlockedBy(blockerId);

		ticketDependencyService.addDependency(ticketId, request, ownerId());

		var dependencies = ticketDependencyService.listDependencies(ticketId);
		assertThat(dependencies).hasSize(1);
		assertThat(dependencies.get(0).getId()).isEqualTo(blockerId);
		assertThat(dependencies.get(0).getStatus()).isEqualTo(TicketStatus.TODO);

		var auditLogs = auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(
				AuditEntityType.DEPENDENCY, ticketDependencyRepository.findAll().get(0).getId());
		assertThat(auditLogs).hasSize(1);
		assertThat(auditLogs.get(0).getAction()).isEqualTo(AuditAction.CREATE);
	}

	@Test
	void addDependency_rejectsMissingBlocker() {
		AddDependencyRequest request = new AddDependencyRequest();
		request.setBlockedBy(999L);

		assertThatThrownBy(() -> ticketDependencyService.addDependency(ticketId, request, ownerId()))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Ticket not found");
	}

	@Test
	void addDependency_rejectsSelfDependency() {
		AddDependencyRequest request = new AddDependencyRequest();
		request.setBlockedBy(ticketId);

		assertThatThrownBy(() -> ticketDependencyService.addDependency(ticketId, request, ownerId()))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("cannot depend on itself");
	}

	@Test
	void addDependency_rejectsDifferentProject() {
		AddDependencyRequest request = new AddDependencyRequest();
		request.setBlockedBy(otherProjectTicketId);

		assertThatThrownBy(() -> ticketDependencyService.addDependency(ticketId, request, ownerId()))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("same project");
	}

	@Test
	void addDependency_rejectsDuplicate() {
		AddDependencyRequest request = new AddDependencyRequest();
		request.setBlockedBy(blockerId);

		ticketDependencyService.addDependency(ticketId, request, ownerId());

		assertThatThrownBy(() -> ticketDependencyService.addDependency(ticketId, request, ownerId()))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("already exists");
	}

	@Test
	void removeDependency_deletesAndWritesAudit() {
		AddDependencyRequest request = new AddDependencyRequest();
		request.setBlockedBy(blockerId);
		ticketDependencyService.addDependency(ticketId, request, ownerId());
		Long dependencyId = ticketDependencyRepository.findAll().get(0).getId();
		auditLogRepository.deleteAll();

		ticketDependencyService.removeDependency(ticketId, blockerId, ownerId());

		assertThat(ticketDependencyService.listDependencies(ticketId)).isEmpty();
		var auditLogs =
				auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(
						AuditEntityType.DEPENDENCY, dependencyId);
		assertThat(auditLogs).hasSize(1);
		assertThat(auditLogs.get(0).getAction()).isEqualTo(AuditAction.DELETE);
	}

	private Long saveTicket(Long projectId, String title) {
		Ticket ticket = new Ticket();
		ticket.setTitle(title);
		ticket.setPriority(TicketPriority.MEDIUM);
		ticket.setType(TicketType.TECHNICAL);
		ticket.setStatus(TicketStatus.TODO);
		ticket.setProjectId(projectId);
		ticket.setOverdue(false);
		return ticketRepository.save(ticket).getId();
	}

	private Long saveUser(String username, Role role) {
		User user = new User();
		user.setUsername(username);
		user.setEmail(username + "@example.com");
		user.setFullName(username);
		user.setRole(role);
		user.setPasswordHash(TestPasswords.encoded());
		return userRepository.save(user).getId();
	}

	private Long ownerId() {
		return userRepository.findAll().get(0).getId();
	}
}
