package com.att.tdp.issueflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.tdp.issueflow.dto.request.CreateCommentRequest;
import com.att.tdp.issueflow.model.entity.Project;
import com.att.tdp.issueflow.model.entity.Ticket;
import com.att.tdp.issueflow.model.entity.User;
import com.att.tdp.issueflow.model.enums.Role;
import com.att.tdp.issueflow.model.enums.TicketPriority;
import com.att.tdp.issueflow.model.enums.TicketStatus;
import com.att.tdp.issueflow.model.enums.TicketType;
import com.att.tdp.issueflow.repository.CommentMentionRepository;
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
class MentionServiceTest {

	@Autowired
	private MentionService mentionService;

	@Autowired
	private CommentService commentService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private CommentMentionRepository commentMentionRepository;

	private Long jdoeId;
	private Long ticketId;

	@BeforeEach
	void setUp() {
		commentMentionRepository.deleteAll();
		commentRepository.deleteAll();
		ticketRepository.deleteAll();
		projectRepository.deleteAll();
		userRepository.deleteAll();

		User author = new User();
		author.setUsername("author");
		author.setEmail("author@example.com");
		author.setFullName("Author");
		author.setRole(Role.DEVELOPER);
		author.setPasswordHash(TestPasswords.encoded());
		Long authorId = userRepository.save(author).getId();

		User jdoe = new User();
		jdoe.setUsername("jdoe");
		jdoe.setEmail("jdoe@example.com");
		jdoe.setFullName("John Doe");
		jdoe.setRole(Role.DEVELOPER);
		jdoe.setPasswordHash(TestPasswords.encoded());
		jdoeId = userRepository.save(jdoe).getId();

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
		Long projectId = projectRepository.save(project).getId();

		Ticket ticket = new Ticket();
		ticket.setTitle("Task");
		ticket.setPriority(TicketPriority.MEDIUM);
		ticket.setType(TicketType.TECHNICAL);
		ticket.setStatus(TicketStatus.TODO);
		ticket.setProjectId(projectId);
		ticket.setAssigneeId(authorId);
		ticket.setOverdue(false);
		ticketId = ticketRepository.save(ticket).getId();

		CreateCommentRequest request = new CreateCommentRequest();
		request.setAuthorId(authorId);
		request.setContent("Hey @jdoe, please review");
		commentService.addComment(ticketId, request, authorId);
	}

	@Test
	void getMentionsForUser_returnsPaginatedComments() {
		var page = mentionService.getMentionsForUser(jdoeId, 1, 10);

		assertThat(page.getTotal()).isEqualTo(1);
		assertThat(page.getPage()).isEqualTo(1);
		assertThat(page.getData()).hasSize(1);
		assertThat(page.getData().get(0).getContent()).contains("@jdoe");
		assertThat(page.getData().get(0).getMentionedUsers()).hasSize(1);
		assertThat(page.getData().get(0).getMentionedUsers().get(0).getUsername()).isEqualTo("jdoe");
	}
}
