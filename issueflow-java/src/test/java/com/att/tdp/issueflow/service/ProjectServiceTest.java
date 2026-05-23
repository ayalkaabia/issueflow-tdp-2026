package com.att.tdp.issueflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.att.tdp.issueflow.dto.request.CreateProjectRequest;
import com.att.tdp.issueflow.dto.request.UpdateProjectRequest;
import com.att.tdp.issueflow.exception.BusinessRuleException;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.model.entity.Ticket;
import com.att.tdp.issueflow.model.entity.User;
import com.att.tdp.issueflow.model.enums.Role;
import com.att.tdp.issueflow.model.enums.TicketPriority;
import com.att.tdp.issueflow.model.enums.TicketStatus;
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
class ProjectServiceTest {

	@Autowired
	private ProjectService projectService;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private UserRepository userRepository;

	private Long ownerId;

	@BeforeEach
	void setUp() {
		ticketRepository.deleteAll();
		projectRepository.deleteAll();
		userRepository.deleteAll();

		User owner = new User();
		owner.setUsername("owner");
		owner.setEmail("owner@example.com");
		owner.setFullName("Owner");
		owner.setRole(Role.ADMIN);
		owner.setPasswordHash(TestPasswords.encoded());
		ownerId = userRepository.save(owner).getId();
	}

	@Test
	void createProject_andGetById() {
		var created = projectService.createProject(createRequest("Sample Project"), ownerId);

		var fetched = projectService.getProjectById(created.getId());

		assertThat(fetched.getName()).isEqualTo("Sample Project");
		assertThat(fetched.getOwnerId()).isEqualTo(ownerId);
	}

	@Test
	void updateProject_changesName() {
		var created = projectService.createProject(createRequest("Sample Project"), ownerId);

		UpdateProjectRequest update = new UpdateProjectRequest();
		update.setName("Updated Name");

		var updated = projectService.updateProject(created.getId(), update, ownerId);

		assertThat(updated.getName()).isEqualTo("Updated Name");
	}

	@Test
	void softDeleteProject_hidesFromActiveList() {
		var created = projectService.createProject(createRequest("Sample Project"), ownerId);

		projectService.softDeleteProject(created.getId(), ownerId);

		assertThat(projectService.getAllProjects()).isEmpty();
		assertThat(projectService.getDeletedProjects(ownerId)).hasSize(1);
	}

	@Test
	void restoreProject_bringsBackActive() {
		var created = projectService.createProject(createRequest("Sample Project"), ownerId);
		projectService.softDeleteProject(created.getId(), ownerId);

		projectService.restoreProject(created.getId(), ownerId);

		assertThat(projectService.getAllProjects()).hasSize(1);
		assertThat(projectService.getProjectById(created.getId()).getName()).isEqualTo("Sample Project");
	}

	@Test
	void getDeletedProjects_rejectsNonAdmin() {
		Long devId = saveDeveloper("dev").getId();
		var created = projectService.createProject(createRequest("Sample Project"), ownerId);
		projectService.softDeleteProject(created.getId(), ownerId);

		assertThatThrownBy(() -> projectService.getDeletedProjects(devId))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("ADMIN");
	}

	@Test
	void restoreProject_rejectsNonAdmin() {
		Long devId = saveDeveloper("dev").getId();
		var created = projectService.createProject(createRequest("Sample Project"), ownerId);
		projectService.softDeleteProject(created.getId(), ownerId);

		assertThatThrownBy(() -> projectService.restoreProject(created.getId(), devId))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("ADMIN");
	}

	@Test
	void getProjectWorkload_countsOpenTicketsPerDeveloper() {
		WorkloadFixture fixture = createWorkloadFixture();
		saveTicket(fixture, "Task 1", fixture.dev1Id(), TicketStatus.TODO);
		saveTicket(fixture, "Task 2", fixture.dev1Id(), TicketStatus.IN_PROGRESS);
		saveTicket(fixture, "Task 3", fixture.dev2Id(), TicketStatus.IN_REVIEW);
		saveTicket(fixture, "Done task", fixture.dev2Id(), TicketStatus.DONE);

		var workload = projectService.getProjectWorkload(fixture.projectId());

		assertThat(workload).hasSize(2);
		assertThat(workload.get(0).getUsername()).isEqualTo("asmith");
		assertThat(workload.get(0).getOpenTicketCount()).isEqualTo(1);
		assertThat(workload.get(1).getUsername()).isEqualTo("jdoe");
		assertThat(workload.get(1).getOpenTicketCount()).isEqualTo(2);
	}

	@Test
	void getProjectWorkload_includesDevelopersWithZeroOpenTickets() {
		WorkloadFixture fixture = createWorkloadFixture();

		var workload = projectService.getProjectWorkload(fixture.projectId());

		assertThat(workload).hasSize(2);
		assertThat(workload).allMatch(entry -> entry.getOpenTicketCount() == 0);
	}

	@Test
	void getProjectWorkload_rejectsMissingProject() {
		assertThatThrownBy(() -> projectService.getProjectWorkload(999L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void createProject_rejectsMissingOwner() {
		CreateProjectRequest request = createRequest("Orphan");
		request.setOwnerId(999L);

		assertThatThrownBy(() -> projectService.createProject(request, ownerId))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void updateProject_rejectsEmptyBody() {
		var created = projectService.createProject(createRequest("Sample Project"), ownerId);

		assertThatThrownBy(() -> projectService.updateProject(created.getId(), new UpdateProjectRequest(), ownerId))
				.isInstanceOf(BusinessRuleException.class);
	}

	private WorkloadFixture createWorkloadFixture() {
		User dev1 = new User();
		dev1.setUsername("jdoe");
		dev1.setEmail("jdoe@example.com");
		dev1.setFullName("John Doe");
		dev1.setRole(Role.DEVELOPER);
		dev1.setPasswordHash(TestPasswords.encoded());
		Long dev1Id = userRepository.save(dev1).getId();

		User dev2 = new User();
		dev2.setUsername("asmith");
		dev2.setEmail("asmith@example.com");
		dev2.setFullName("Ann Smith");
		dev2.setRole(Role.DEVELOPER);
		dev2.setPasswordHash(TestPasswords.encoded());
		Long dev2Id = userRepository.save(dev2).getId();

		Long projectId = projectService.createProject(createRequest("Workload Project"), ownerId).getId();
		return new WorkloadFixture(projectId, dev1Id, dev2Id);
	}

	private void saveTicket(WorkloadFixture fixture, String title, Long assigneeId, TicketStatus status) {
		Ticket ticket = new Ticket();
		ticket.setTitle(title);
		ticket.setPriority(TicketPriority.MEDIUM);
		ticket.setType(TicketType.BUG);
		ticket.setStatus(status);
		ticket.setProjectId(fixture.projectId());
		ticket.setAssigneeId(assigneeId);
		ticket.setOverdue(false);
		ticketRepository.save(ticket);
	}

	private record WorkloadFixture(Long projectId, Long dev1Id, Long dev2Id) {}

	private User saveDeveloper(String username) {
		User dev = new User();
		dev.setUsername(username);
		dev.setEmail(username + "@example.com");
		dev.setFullName("Developer");
		dev.setRole(Role.DEVELOPER);
		dev.setPasswordHash(TestPasswords.encoded());
		return userRepository.save(dev);
	}

	private CreateProjectRequest createRequest(String name) {
		CreateProjectRequest request = new CreateProjectRequest();
		request.setName(name);
		request.setDescription("A sample project");
		request.setOwnerId(ownerId);
		return request;
	}
}
