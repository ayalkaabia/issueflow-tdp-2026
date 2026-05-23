package com.att.tdp.issueflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.tdp.issueflow.dto.request.CreateCommentRequest;
import com.att.tdp.issueflow.dto.request.CreateTicketRequest;
import com.att.tdp.issueflow.dto.response.AuditLogResponse;
import com.att.tdp.issueflow.model.entity.Project;
import com.att.tdp.issueflow.model.entity.User;
import com.att.tdp.issueflow.model.enums.AuditAction;
import com.att.tdp.issueflow.model.enums.AuditActor;
import com.att.tdp.issueflow.model.enums.AuditEntityType;
import com.att.tdp.issueflow.model.enums.Role;
import com.att.tdp.issueflow.model.enums.TicketPriority;
import com.att.tdp.issueflow.model.enums.TicketType;
import com.att.tdp.issueflow.repository.AuditLogRepository;
import com.att.tdp.issueflow.repository.CommentRepository;
import com.att.tdp.issueflow.repository.ProjectRepository;
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
class AuditLogServiceTest {

	@Autowired
	private AuditLogService auditLogService;

	@Autowired
	private TicketService ticketService;

	@Autowired
	private CommentService commentService;

	@Autowired
	private AuditLogRepository auditLogRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private CommentRepository commentRepository;

	private Long projectId;
	private Long developerId;
	private Long ownerId;

	@BeforeEach
	void setUp() {
		auditLogRepository.deleteAll();
		commentRepository.deleteAll();
		ticketRepository.deleteAll();
		projectRepository.deleteAll();
		userRepository.deleteAll();

		developerId = saveUser("dev1", "dev1@example.com", Role.DEVELOPER);
		saveUser("dev2", "dev2@example.com", Role.DEVELOPER);
		ownerId = saveUser("owner", "owner@example.com", Role.ADMIN);

		Project project = new Project();
		project.setName("Test");
		project.setDescription("Desc");
		project.setOwnerId(ownerId);
		projectId = projectRepository.save(project).getId();
	}

	@Test
	void ticketCreate_writesCreateAndAutoAssignAuditLogs() {
		var ticket = ticketService.createTicket(createTicketRequest(), ownerId);

		var logs = auditLogService.getAuditLogs(AuditEntityType.TICKET, ticket.getId(), null, null);

		assertThat(logs).extracting(AuditLogResponse::getAction)
				.containsExactly(AuditAction.AUTO_ASSIGN, AuditAction.CREATE);
		assertThat(logs).extracting(AuditLogResponse::getActor)
				.containsExactly(AuditActor.SYSTEM, AuditActor.USER);
	}

	@Test
	void commentAdd_writesCreateAuditLog() {
		var ticket = ticketService.createTicket(createTicketRequest(), ownerId);
		auditLogRepository.deleteAll();

		CreateCommentRequest request = new CreateCommentRequest();
		request.setAuthorId(developerId);
		request.setContent("Note");
		var comment = commentService.addComment(ticket.getId(), request, ownerId);

		var logs = auditLogService.getAuditLogs(AuditEntityType.COMMENT, comment.getId(), null, null);

		assertThat(logs).hasSize(1);
		assertThat(logs.get(0).getAction()).isEqualTo(AuditAction.CREATE);
		assertThat(logs.get(0).getActor()).isEqualTo(AuditActor.USER);
	}

	@Test
	void getAuditLogs_filtersByAction() {
		ticketService.createTicket(createTicketRequest(), ownerId);

		var createLogs = auditLogService.getAuditLogs(null, null, AuditAction.CREATE, null);
		var autoAssignLogs =
				auditLogService.getAuditLogs(null, null, AuditAction.AUTO_ASSIGN, AuditActor.SYSTEM);

		assertThat(createLogs).isNotEmpty();
		assertThat(autoAssignLogs).isNotEmpty();
		assertThat(createLogs).allMatch(log -> log.getAction() == AuditAction.CREATE);
		assertThat(autoAssignLogs).allMatch(log -> log.getAction() == AuditAction.AUTO_ASSIGN);
	}

	private CreateTicketRequest createTicketRequest() {
		CreateTicketRequest request = new CreateTicketRequest();
		request.setTitle("Task");
		request.setPriority(TicketPriority.MEDIUM);
		request.setType(TicketType.TECHNICAL);
		request.setProjectId(projectId);
		return request;
	}

	private Long saveUser(String username, String email, Role role) {
		User user = new User();
		user.setUsername(username);
		user.setEmail(email);
		user.setFullName(username);
		user.setRole(role);
		user.setPasswordHash(TestPasswords.encoded());
		return userRepository.save(user).getId();
	}
}
