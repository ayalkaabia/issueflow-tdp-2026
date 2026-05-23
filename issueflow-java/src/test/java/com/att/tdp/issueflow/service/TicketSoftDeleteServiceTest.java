package com.att.tdp.issueflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.att.tdp.issueflow.dto.request.CreateTicketRequest;
import com.att.tdp.issueflow.exception.BusinessRuleException;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.model.entity.Project;
import com.att.tdp.issueflow.model.entity.User;
import com.att.tdp.issueflow.model.enums.Role;
import com.att.tdp.issueflow.model.enums.TicketPriority;
import com.att.tdp.issueflow.model.enums.TicketType;
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
class TicketSoftDeleteServiceTest {

	@Autowired
	private TicketService ticketService;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private UserRepository userRepository;

	private Long projectId;
	private Long adminId;

	@BeforeEach
	void setUp() {
		ticketRepository.deleteAll();
		projectRepository.deleteAll();
		userRepository.deleteAll();

		User admin = new User();
		admin.setUsername("admin");
		admin.setEmail("admin@example.com");
		admin.setFullName("Admin");
		admin.setRole(Role.ADMIN);
		admin.setPasswordHash(TestPasswords.encoded());
		adminId = userRepository.save(admin).getId();

		Project project = new Project();
		project.setName("Main");
		project.setOwnerId(adminId);
		projectId = projectRepository.save(project).getId();
	}

	@Test
	void softDeleteTicket_hidesFromNormalGetAndList() {
		var created = ticketService.createTicket(createRequest("Task A"), adminId);

		ticketService.softDeleteTicket(created.getId(), adminId);

		assertThat(ticketService.getTicketsByProject(projectId)).isEmpty();
		assertThatThrownBy(() -> ticketService.getTicketById(created.getId()))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void softDeleteTicket_appearsInDeletedList() {
		var created = ticketService.createTicket(createRequest("Task A"), adminId);

		ticketService.softDeleteTicket(created.getId(), adminId);

		var deleted = ticketService.getDeletedTickets(projectId, adminId);
		assertThat(deleted).hasSize(1);
		assertThat(deleted.get(0).getId()).isEqualTo(created.getId());
	}

	@Test
	void restoreTicket_makesVisibleAgain() {
		var created = ticketService.createTicket(createRequest("Task A"), adminId);
		ticketService.softDeleteTicket(created.getId(), adminId);

		var restored = ticketService.restoreTicket(created.getId(), adminId);

		assertThat(restored.getTitle()).isEqualTo("Task A");
		assertThat(ticketService.getTicketsByProject(projectId)).hasSize(1);
		assertThat(ticketService.getTicketById(created.getId()).getTitle()).isEqualTo("Task A");
	}

	@Test
	void getDeletedTickets_rejectsNonAdmin() {
		Long devId = saveDeveloper("dev").getId();
		var created = ticketService.createTicket(createRequest("Task A"), adminId);
		ticketService.softDeleteTicket(created.getId(), adminId);

		assertThatThrownBy(() -> ticketService.getDeletedTickets(projectId, devId))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("ADMIN");
	}

	@Test
	void restoreTicket_rejectsNonAdmin() {
		Long devId = saveDeveloper("dev2").getId();
		var created = ticketService.createTicket(createRequest("Task A"), adminId);
		ticketService.softDeleteTicket(created.getId(), adminId);

		assertThatThrownBy(() -> ticketService.restoreTicket(created.getId(), devId))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("ADMIN");
	}

	private User saveDeveloper(String username) {
		User dev = new User();
		dev.setUsername(username);
		dev.setEmail(username + "@example.com");
		dev.setFullName("Dev");
		dev.setRole(Role.DEVELOPER);
		dev.setPasswordHash(TestPasswords.encoded());
		return userRepository.save(dev);
	}

	private CreateTicketRequest createRequest(String title) {
		CreateTicketRequest request = new CreateTicketRequest();
		request.setTitle(title);
		request.setPriority(TicketPriority.MEDIUM);
		request.setType(TicketType.TECHNICAL);
		request.setProjectId(projectId);
		return request;
	}
}
