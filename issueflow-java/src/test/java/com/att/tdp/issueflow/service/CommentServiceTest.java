package com.att.tdp.issueflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.att.tdp.issueflow.dto.request.CreateCommentRequest;
import com.att.tdp.issueflow.dto.request.UpdateCommentRequest;
import com.att.tdp.issueflow.exception.BusinessRuleException;
import com.att.tdp.issueflow.exception.OptimisticLockConflictException;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
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
class CommentServiceTest {

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

	private Long ticketId;
	private Long authorId;
	private Long jdoeId;

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
		authorId = userRepository.save(author).getId();

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
	}

	@Test
	void addComment_andFetchByTicket() {
		CreateCommentRequest request = new CreateCommentRequest();
		request.setAuthorId(authorId);
		request.setContent("Hello world");

		var created = commentService.addComment(ticketId, request, authorId);
		var comments = commentService.getCommentsByTicket(ticketId);

		assertThat(created.getContent()).isEqualTo("Hello world");
		assertThat(comments).hasSize(1);
		assertThat(comments.get(0).getAuthorId()).isEqualTo(authorId);
	}

	@Test
	void updateComment_rejectsStaleVersion() {
		var created = commentService.addComment(ticketId, createRequest(), authorId);

		UpdateCommentRequest update = new UpdateCommentRequest();
		update.setContent("Updated");
		update.setVersion(created.getVersion() + 1);

		assertThatThrownBy(() -> commentService.updateComment(ticketId, created.getId(), update, authorId))
				.isInstanceOf(OptimisticLockConflictException.class);
	}

	@Test
	void deleteComment_removesComment() {
		var created = commentService.addComment(ticketId, createRequest(), authorId);

		commentService.deleteComment(ticketId, created.getId(), authorId);

		assertThat(commentRepository.findById(created.getId())).isEmpty();
	}

	@Test
	void addComment_persistsMentions() {
		CreateCommentRequest request = new CreateCommentRequest();
		request.setAuthorId(authorId);
		request.setContent("Hello @jdoe!");

		var created = commentService.addComment(ticketId, request, authorId);

		assertThat(created.getMentionedUsers()).hasSize(1);
		assertThat(created.getMentionedUsers().get(0).getUsername()).isEqualTo("jdoe");
		assertThat(created.getMentionedUsers().get(0).getId()).isEqualTo(jdoeId);
	}

	@Test
	void addComment_rejectsUnknownMention() {
		CreateCommentRequest request = new CreateCommentRequest();
		request.setAuthorId(authorId);
		request.setContent("Hello @nobody!");

		assertThatThrownBy(() -> commentService.addComment(ticketId, request, authorId))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void updateComment_replacesMentions() {
		var created = commentService.addComment(ticketId, createRequest(), authorId);

		UpdateCommentRequest update = new UpdateCommentRequest();
		update.setContent("Ping @jdoe");
		update.setVersion(created.getVersion());

		var updated = commentService.updateComment(ticketId, created.getId(), update, authorId);

		assertThat(updated.getMentionedUsers()).hasSize(1);
		assertThat(updated.getMentionedUsers().get(0).getUsername()).isEqualTo("jdoe");
	}

	@Test
	void addComment_rejectsMissingTicket() {
		assertThatThrownBy(() -> commentService.addComment(999L, createRequest(), authorId))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	private CreateCommentRequest createRequest() {
		CreateCommentRequest request = new CreateCommentRequest();
		request.setAuthorId(authorId);
		request.setContent("Test comment");
		return request;
	}
}
