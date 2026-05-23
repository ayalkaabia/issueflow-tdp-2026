package com.att.tdp.issueflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.att.tdp.issueflow.exception.BusinessRuleException;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TicketCsvServiceTest {

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

		String content = new String(ticketCsvService.exportTickets(projectId), StandardCharsets.UTF_8);

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
	void importTickets_createsValidRows() {
		String csv =
				"""
				title,description,status,priority,type,assigneeId
				Imported task,From CSV,TODO,HIGH,BUG,
				Second task,,,MEDIUM,TECHNICAL,%d
				"""
						.formatted(developerId);

		var result = ticketCsvService.importTickets(projectId, csvFile(csv), ownerId);

		assertThat(result.getCreated()).isEqualTo(2);
		assertThat(result.getFailed()).isZero();
		assertThat(ticketRepository.findByProjectIdAndDeletedAtIsNull(projectId)).hasSize(3);
	}

	@Test
	void importTickets_handlesQuotedCommasAndQuotes() {
		String csv = "title,description,status,priority,type,assigneeId\n"
				+ "\"Title, with comma\",\"Desc \"\"quoted\"\"\",TODO,MEDIUM,TECHNICAL,\n";

		var result = ticketCsvService.importTickets(projectId, csvFile(csv), ownerId);

		assertThat(result.getCreated()).isEqualTo(1);
		var imported = ticketRepository.findByProjectIdAndDeletedAtIsNull(projectId).stream()
				.filter(ticket -> ticket.getTitle().contains("comma"))
				.findFirst()
				.orElseThrow();
		assertThat(imported.getDescription()).isEqualTo("Desc \"quoted\"");
	}

	@Test
	void importTickets_reportsInvalidRowsWithoutStopping() {
		String csv =
				"""
				title,description,status,priority,type,assigneeId
				Valid task,,,MEDIUM,TECHNICAL,
				,Missing title,,MEDIUM,TECHNICAL,
				Bad priority,,,URGENT,TECHNICAL,
				""";

		var result = ticketCsvService.importTickets(projectId, csvFile(csv), ownerId);

		assertThat(result.getCreated()).isEqualTo(1);
		assertThat(result.getFailed()).isEqualTo(2);
		assertThat(result.getErrors()).hasSize(2);
	}

	@Test
	void importTickets_rejectsNonCsvFile() {
		MockMultipartFile file =
				new MockMultipartFile("file", "data.json", "application/json", "{}".getBytes());

		assertThatThrownBy(() -> ticketCsvService.importTickets(projectId, file, ownerId))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("CSV");
	}

	@Test
	void importTickets_rejectsMissingProject() {
		assertThatThrownBy(() -> ticketCsvService.importTickets(999L, csvFile(minimalCsv()), ownerId))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	private MockMultipartFile csvFile(String content) {
		return new MockMultipartFile(
				"file", "tickets.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
	}

	private String minimalCsv() {
		return """
				title,description,status,priority,type,assigneeId
				Imported only,,,MEDIUM,TECHNICAL,
				""";
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
