package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.request.CreateCommentRequest;
import com.att.tdp.issueflow.dto.request.UpdateCommentRequest;
import com.att.tdp.issueflow.dto.response.CommentResponse;
import com.att.tdp.issueflow.dto.response.MentionedUserResponse;
import com.att.tdp.issueflow.exception.OptimisticLockConflictException;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.mapper.CommentMapper;
import com.att.tdp.issueflow.model.entity.Comment;
import com.att.tdp.issueflow.model.enums.AuditAction;
import com.att.tdp.issueflow.model.enums.AuditActor;
import com.att.tdp.issueflow.model.enums.AuditEntityType;
import com.att.tdp.issueflow.repository.CommentMentionRepository;
import com.att.tdp.issueflow.repository.CommentRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

	private final CommentRepository commentRepository;
	private final CommentMentionRepository commentMentionRepository;
	private final CommentMentionService commentMentionService;
	private final TicketRepository ticketRepository;
	private final UserRepository userRepository;
	private final AuditService auditService;

	@Transactional
	public CommentResponse addComment(Long ticketId, CreateCommentRequest request, Long performedBy) {
		requireActiveTicket(ticketId);
		requireUser(request.getAuthorId());

		Comment comment = new Comment();
		comment.setTicketId(ticketId);
		comment.setAuthorId(request.getAuthorId());
		comment.setContent(request.getContent());

		Comment saved = commentRepository.save(comment);
		commentMentionService.syncMentions(saved.getId(), saved.getContent());
		auditService.log(
				AuditAction.CREATE, AuditEntityType.COMMENT, saved.getId(), performedBy, AuditActor.USER);
		return toResponseWithMentions(saved);
	}

	@Transactional(readOnly = true)
	public List<CommentResponse> getCommentsByTicket(Long ticketId) {
		requireActiveTicket(ticketId);
		List<Comment> comments = commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
		return toResponsesWithMentions(comments);
	}

	@Transactional
	public CommentResponse updateComment(
			Long ticketId, Long commentId, UpdateCommentRequest request, Long performedBy) {
		requireActiveTicket(ticketId);
		Comment comment = requireCommentForTicket(ticketId, commentId);

		if (!Objects.equals(comment.getVersion(), request.getVersion())) {
			throw new OptimisticLockConflictException(
					"Comment was modified by another user; refresh and retry with the current version");
		}

		comment.setContent(request.getContent());

		try {
			Comment saved = commentRepository.saveAndFlush(comment);
			commentMentionService.syncMentions(saved.getId(), saved.getContent());
			auditService.log(
					AuditAction.UPDATE, AuditEntityType.COMMENT, saved.getId(), performedBy, AuditActor.USER);
			return toResponseWithMentions(saved);
		} catch (OptimisticLockingFailureException ex) {
			throw new OptimisticLockConflictException(
					"Comment was modified by another user; refresh and retry with the current version", ex);
		}
	}

	@Transactional
	public void deleteComment(Long ticketId, Long commentId, Long performedBy) {
		requireActiveTicket(ticketId);
		Comment comment = requireCommentForTicket(ticketId, commentId);

		commentMentionRepository.deleteByCommentId(commentId);
		commentRepository.delete(comment);

		auditService.log(
				AuditAction.DELETE, AuditEntityType.COMMENT, comment.getId(), performedBy, AuditActor.USER);
	}

	private CommentResponse toResponseWithMentions(Comment comment) {
		List<MentionedUserResponse> mentionedUsers = commentMentionService.getMentionedUsers(comment.getId());
		return CommentMapper.toResponse(comment, mentionedUsers);
	}

	private List<CommentResponse> toResponsesWithMentions(List<Comment> comments) {
		if (comments.isEmpty()) {
			return List.of();
		}
		List<Long> commentIds = comments.stream().map(Comment::getId).toList();
		Map<Long, List<MentionedUserResponse>> mentionsByCommentId =
				commentMentionService.getMentionedUsersByCommentIds(commentIds);
		return comments.stream()
				.map(comment -> CommentMapper.toResponse(
						comment, mentionsByCommentId.getOrDefault(comment.getId(), List.of())))
				.toList();
	}

	private Comment requireCommentForTicket(Long ticketId, Long commentId) {
		return commentRepository
				.findByIdAndTicketId(commentId, ticketId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Comment not found for ticket " + ticketId + ": " + commentId));
	}

	private void requireActiveTicket(Long ticketId) {
		ticketRepository
				.findByIdAndDeletedAtIsNull(ticketId)
				.orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));
	}

	private void requireUser(Long userId) {
		userRepository
				.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
	}
}
