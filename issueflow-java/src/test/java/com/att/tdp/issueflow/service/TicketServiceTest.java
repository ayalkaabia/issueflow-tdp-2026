package com.att.tdp.issueflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.att.tdp.issueflow.dto.request.CreateTicketRequest;
import com.att.tdp.issueflow.dto.request.UpdateTicketRequest;
import com.att.tdp.issueflow.exception.BusinessRuleException;
import com.att.tdp.issueflow.model.entity.Project;
import com.att.tdp.issueflow.model.entity.TicketDependency;
import com.att.tdp.issueflow.model.entity.User;
import com.att.tdp.issueflow.model.enums.Role;
import com.att.tdp.issueflow.model.enums.TicketPriority;
import com.att.tdp.issueflow.model.enums.TicketStatus;
import com.att.tdp.issueflow.model.enums.TicketType;
import com.att.tdp.issueflow.repository.ProjectRepository;
import com.att.tdp.issueflow.repository.TicketDependencyRepository;
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
class TicketServiceTest {

	@Autowired
	private TicketService ticketService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private TicketDependencyRepository ticketDependencyRepository;

	private Long projectId;
	private Long developerId;

	@BeforeEach
	void setUp() {
		ticketDependencyRepository.deleteAll();
		ticketRepository.deleteAll();
		projectRepository.deleteAll();
		userRepository.deleteAll();

		User developer = new User();
		developer.setUsername("dev1");
		developer.setEmail("dev1@example.com");
		developer.setFullName("Dev One");
		developer.setRole(Role.DEVELOPER);
		developer.setPasswordHash(TestPasswords.encoded());
		developerId = userRepository.save(developer).getId();

		User developer2 = new User();
		developer2.setUsername("dev2");
		developer2.setEmail("dev2@example.com");
		developer2.setFullName("Dev Two");
		developer2.setRole(Role.DEVELOPER);
		developer2.setPasswordHash(TestPasswords.encoded());
		userRepository.save(developer2);

		User owner = new User();
		owner.setUsername("owner");
		owner.setEmail("owner@example.com");
		owner.setFullName("Owner");
		owner.setRole(Role.ADMIN);
		owner.setPasswordHash(TestPasswords.encoded());
		Long ownerId = userRepository.save(owner).getId();

		Project project = new Project();
		project.setName("Test");
		project.setDescription("Desc");
		project.setOwnerId(ownerId);
		projectId = projectRepository.save(project).getId();
	}

	@Test
	void createTicket_autoAssignsLeastLoadedDeveloper() {
		CreateTicketRequest request = new CreateTicketRequest();
		request.setTitle("Bug");
		request.setPriority(TicketPriority.HIGH);
		request.setType(TicketType.BUG);
		request.setProjectId(projectId);

		var response = ticketService.createTicket(request, ownerId());

		assertThat(response.getAssigneeId()).isEqualTo(developerId);
	}

	@Test
	void createTicket_marksOverdueWhenDueDateInPast() {
		CreateTicketRequest request = createBasicRequest();
		request.setDueDate(Instant.parse("2020-01-01T00:00:00Z"));

		var response = ticketService.createTicket(request, ownerId());

		assertThat(response.isOverdue()).isTrue();
	}

	@Test
	void updateTicket_rejectsInvalidStatusTransition() {
		var created = ticketService.createTicket(createBasicRequest(), ownerId());

		UpdateTicketRequest update = new UpdateTicketRequest();
		update.setStatus(TicketStatus.DONE);
		update.setVersion(created.getVersion());

		assertThatThrownBy(() -> ticketService.updateTicket(created.getId(), update, ownerId()))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Invalid status transition");
	}

	@Test
	void updateTicket_rejectsDoneWhenBlockersUnresolved() {
		var ticket = ticketService.createTicket(createBasicRequest(), ownerId());
		var blocker = ticketService.createTicket(createBasicRequest(), ownerId());

		TicketDependency dependency = new TicketDependency();
		dependency.setTicketId(ticket.getId());
		dependency.setBlockedById(blocker.getId());
		ticketDependencyRepository.save(dependency);

		advanceToInReview(ticket.getId());

		UpdateTicketRequest toDone = new UpdateTicketRequest();
		toDone.setStatus(TicketStatus.DONE);
		toDone.setVersion(currentVersion(ticket.getId()));

		assertThatThrownBy(() -> ticketService.updateTicket(ticket.getId(), toDone, ownerId()))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("unresolved blockers");
	}

	@Test
	void updateTicket_allowsDoneWhenAllBlockersDone() {
		var ticket = ticketService.createTicket(createBasicRequest(), ownerId());
		var blocker = ticketService.createTicket(createBasicRequest(), ownerId());

		TicketDependency dependency = new TicketDependency();
		dependency.setTicketId(ticket.getId());
		dependency.setBlockedById(blocker.getId());
		ticketDependencyRepository.save(dependency);

		advanceToDone(blocker.getId());
		advanceToInReview(ticket.getId());

		UpdateTicketRequest toDone = new UpdateTicketRequest();
		toDone.setStatus(TicketStatus.DONE);
		toDone.setVersion(currentVersion(ticket.getId()));

		var updated = ticketService.updateTicket(ticket.getId(), toDone, ownerId());
		assertThat(updated.getStatus()).isEqualTo(TicketStatus.DONE);
	}

	@Test
	void updateTicket_clearsOverdueWhenPriorityChanges() {
		CreateTicketRequest request = createBasicRequest();
		request.setDueDate(Instant.parse("2020-01-01T00:00:00Z"));
		var created = ticketService.createTicket(request, ownerId());
		assertThat(created.isOverdue()).isTrue();

		UpdateTicketRequest update = new UpdateTicketRequest();
		update.setPriority(TicketPriority.LOW);
		update.setVersion(created.getVersion());

		var updated = ticketService.updateTicket(created.getId(), update, ownerId());
		assertThat(updated.isOverdue()).isFalse();
	}

	@Test
	void updateTicket_rejectsUpdatesOnDoneTicket() {
		var created = ticketService.createTicket(createBasicRequest(), ownerId());
		advanceToDone(created.getId());

		UpdateTicketRequest update = new UpdateTicketRequest();
		update.setTitle("New title");
		update.setVersion(currentVersion(created.getId()));

		assertThatThrownBy(() -> ticketService.updateTicket(created.getId(), update, ownerId()))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("DONE tickets cannot be updated");
	}

	private void advanceToDone(Long ticketId) {
		advanceToInReview(ticketId);

		UpdateTicketRequest toDone = new UpdateTicketRequest();
		toDone.setStatus(TicketStatus.DONE);
		toDone.setVersion(currentVersion(ticketId));
		ticketService.updateTicket(ticketId, toDone, ownerId());
	}

	private void advanceToInReview(Long ticketId) {
		UpdateTicketRequest inProgress = new UpdateTicketRequest();
		inProgress.setStatus(TicketStatus.IN_PROGRESS);
		inProgress.setVersion(currentVersion(ticketId));
		ticketService.updateTicket(ticketId, inProgress, ownerId());

		UpdateTicketRequest inReview = new UpdateTicketRequest();
		inReview.setStatus(TicketStatus.IN_REVIEW);
		inReview.setVersion(currentVersion(ticketId));
		ticketService.updateTicket(ticketId, inReview, ownerId());
	}

	private Long currentVersion(Long ticketId) {
		return ticketService.getTicketById(ticketId).getVersion();
	}

	private CreateTicketRequest createBasicRequest() {
		CreateTicketRequest request = new CreateTicketRequest();
		request.setTitle("Task");
		request.setPriority(TicketPriority.MEDIUM);
		request.setType(TicketType.TECHNICAL);
		request.setProjectId(projectId);
		return request;
	}

	private Long ownerId() {
		return userRepository.findAll().stream()
				.filter(u -> u.getRole() == Role.ADMIN)
				.findFirst()
				.orElseThrow()
				.getId();
	}
}
