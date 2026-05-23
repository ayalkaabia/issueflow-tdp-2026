package com.att.tdp.issueflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.model.entity.Project;
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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TicketCsvExportServiceTest {

	@Autowired
	private TicketCsvService ticketCsvService;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private UserRepository userRepository;

	private Long projectId;
	private Long otherProjectId;
	private Long developerId;

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
		Long ownerId = userRepository.save(owner).getId();

		User developer = new User();
		developer.setUsername("jdoe");
		developer.setEmail("jdoe@example.com");
		developer.setFullName("John Doe");
		developer.setRole(Role.DEVELOPER);
		developer.setPasswordHash(TestPasswords.encoded());
		developerId = userRepository.save(developer).getId();

		Project project = new Project();
		project.setName("Main");
		project.setOwnerId(ownerId);
		projectId = projectRepository.save(project).getId();

		Project other = new Project();
		other.setName("Other");
		other.setOwnerId(ownerId);
		otherProjectId = projectRepository.save(other).getId();

		saveTicket(projectId, "Existing", "Desc", TicketStatus.TODO, developerId);
	}

	@Test
	void exportTickets_includesHeaderAndProjectTicketsOnly() {
		saveTicket(otherProjectId, "Other project", "X", TicketStatus.TODO, null);

		byte[] csv = ticketCsvService.exportTickets(projectId);
		String content = new String(csv, StandardCharsets.UTF_8);

		assertThat(content).startsWith("id,title,description,status,priority,type,assigneeId");
		assertThat(content).contains("Existing");
		assertThat(content).doesNotContain("Other project");
	}

	@Test
	void exportTickets_excludesSoftDeletedTickets() {
		Ticket deleted = saveTicket(projectId, "Deleted", "gone", TicketStatus.TODO, null);
		deleted.setDeletedAt(Instant.now());
		ticketRepository.save(deleted);

		String content = new String(ticketCsvService.exportTickets(projectId), StandardCharsets.UTF_8);

		assertThat(content).contains("Existing");
		assertThat(content).doesNotContain("Deleted");
	}

	@Test
	void exportTickets_escapesCommasAndQuotes() {
		saveTicket(projectId, "Fix \"login\", now", "a,b", TicketStatus.IN_PROGRESS, developerId);

		String content = new String(ticketCsvService.exportTickets(projectId), StandardCharsets.UTF_8);

		assertThat(content).contains("\"Fix \"\"login\"\", now\"");
		assertThat(content).contains("\"a,b\"");
	}

	@Test
	void exportTickets_rejectsMissingProject() {
		assertThatThrownBy(() -> ticketCsvService.exportTickets(999L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void exportTickets_rejectsSoftDeletedProject() {
		Project project = projectRepository.findById(projectId).orElseThrow();
		project.setDeletedAt(Instant.now());
		projectRepository.save(project);

		assertThatThrownBy(() -> ticketCsvService.exportTickets(projectId))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	private Ticket saveTicket(
			Long project, String title, String description, TicketStatus status, Long assigneeId) {
		Ticket ticket = new Ticket();
		ticket.setTitle(title);
		ticket.setDescription(description);
		ticket.setPriority(TicketPriority.MEDIUM);
		ticket.setType(TicketType.TECHNICAL);
		ticket.setStatus(status);
		ticket.setProjectId(project);
		ticket.setAssigneeId(assigneeId);
		ticket.setOverdue(false);
		return ticketRepository.save(ticket);
	}
}
