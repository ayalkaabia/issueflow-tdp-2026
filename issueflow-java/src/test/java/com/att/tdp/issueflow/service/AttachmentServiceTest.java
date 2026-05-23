package com.att.tdp.issueflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.att.tdp.issueflow.repository.AttachmentRepository;
import com.att.tdp.issueflow.repository.AuditLogRepository;
import com.att.tdp.issueflow.repository.ProjectRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import com.att.tdp.issueflow.support.TestPasswords;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AttachmentServiceTest {

	@TempDir
	static Path tempUploadDir;

	@DynamicPropertySource
	static void uploadDir(DynamicPropertyRegistry registry) {
		registry.add("issueflow.attachments.upload-dir", () -> tempUploadDir.toString());
	}

	@Autowired
	private AttachmentService attachmentService;

	@Autowired
	private AttachmentRepository attachmentRepository;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AuditLogRepository auditLogRepository;

	private Long ticketId;

	@BeforeEach
	void setUp() {
		auditLogRepository.deleteAll();
		attachmentRepository.deleteAll();
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
		Long projectId = projectRepository.save(project).getId();

		Ticket ticket = new Ticket();
		ticket.setTitle("Task");
		ticket.setPriority(TicketPriority.MEDIUM);
		ticket.setType(TicketType.TECHNICAL);
		ticket.setStatus(TicketStatus.TODO);
		ticket.setProjectId(projectId);
		ticket.setOverdue(false);
		ticketId = ticketRepository.save(ticket).getId();
	}

	@Test
	void uploadAttachment_acceptsTxt() {
		MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes());
		assertThat(attachmentService.uploadAttachment(ticketId, file, ownerId()).getContentType())
				.isEqualTo("text/plain");
	}

	@Test
	void uploadAttachment_rejectsOversizedFile() {
		byte[] tooLarge = new byte[(int) AttachmentService.MAX_FILE_SIZE_BYTES + 1];
		MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", tooLarge);

		assertThatThrownBy(() -> attachmentService.uploadAttachment(ticketId, file, ownerId()))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("10 MB");
	}

	@Test
	void uploadAttachment_rejectsUnsupportedType() {
		MockMultipartFile file =
				new MockMultipartFile("file", "archive.zip", "application/zip", "zip".getBytes());

		assertThatThrownBy(() -> attachmentService.uploadAttachment(ticketId, file, ownerId()))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Unsupported file type");
	}

	@Test
	void uploadAttachment_persistsMetadataAndFile() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes());

		var response = attachmentService.uploadAttachment(ticketId, file, ownerId());

		var saved = attachmentRepository.findById(response.getId()).orElseThrow();
		assertThat(saved.getOriginalFileName()).isEqualTo("note.txt");
		assertThat(saved.getSizeBytes()).isEqualTo(5);

		Path storedFile = tempUploadDir.resolve(saved.getStoragePath());
		assertThat(Files.exists(storedFile)).isTrue();
		assertThat(Files.readString(storedFile)).isEqualTo("hello");
	}

	@Test
	void deleteAttachment_removesRecordAndFile() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes());
		var response = attachmentService.uploadAttachment(ticketId, file, ownerId());
		var saved = attachmentRepository.findById(response.getId()).orElseThrow();
		Path storedFile = tempUploadDir.resolve(saved.getStoragePath());

		attachmentService.deleteAttachment(ticketId, response.getId(), ownerId());

		assertThat(attachmentRepository.findById(response.getId())).isEmpty();
		assertThat(Files.exists(storedFile)).isFalse();
	}

	@Test
	void uploadAttachment_rejectsSoftDeletedTicket() {
		Ticket ticket = ticketRepository.findById(ticketId).orElseThrow();
		ticket.setDeletedAt(Instant.now());
		ticketRepository.save(ticket);

		MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes());

		assertThatThrownBy(() -> attachmentService.uploadAttachment(ticketId, file, ownerId()))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void uploadAttachment_writesAuditLog() {
		MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes());

		var response = attachmentService.uploadAttachment(ticketId, file, ownerId());

		var logs = auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(
				AuditEntityType.ATTACHMENT, response.getId());
		assertThat(logs).hasSize(1);
		assertThat(logs.get(0).getAction()).isEqualTo(AuditAction.CREATE);
	}

	private Long ownerId() {
		return userRepository.findAll().get(0).getId();
	}
}
