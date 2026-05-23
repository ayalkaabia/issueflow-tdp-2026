package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.response.CommentResponse;
import com.att.tdp.issueflow.dto.response.MentionPageResponse;
import com.att.tdp.issueflow.dto.response.MentionedUserResponse;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.mapper.CommentMapper;
import com.att.tdp.issueflow.model.entity.Comment;
import com.att.tdp.issueflow.repository.CommentMentionRepository;
import com.att.tdp.issueflow.repository.CommentRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MentionService {

	private static final int DEFAULT_PAGE = 1;
	private static final int DEFAULT_PAGE_SIZE = 10;

	private final CommentMentionRepository commentMentionRepository;
	private final CommentRepository commentRepository;
	private final CommentMentionService commentMentionService;
	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public MentionPageResponse getMentionsForUser(Long userId, Integer page, Integer pageSize) {
		requireUser(userId);

		int resolvedPage = page == null || page < 1 ? DEFAULT_PAGE : page;
		int resolvedPageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : pageSize;

		Page<Long> commentIdPage = commentMentionRepository.findCommentIdsByMentionedUserId(
				userId, PageRequest.of(resolvedPage - 1, resolvedPageSize));

		List<Long> commentIds = commentIdPage.getContent();
		if (commentIds.isEmpty()) {
			return MentionPageResponse.builder()
					.data(List.of())
					.total(commentIdPage.getTotalElements())
					.page(resolvedPage)
					.build();
		}

		Map<Long, Comment> commentsById = commentRepository.findAllById(commentIds).stream()
				.collect(Collectors.toMap(Comment::getId, Function.identity()));
		Map<Long, List<MentionedUserResponse>> mentionsByCommentId =
				commentMentionService.getMentionedUsersByCommentIds(commentIds);

		List<CommentResponse> data = commentIds.stream()
				.map(commentsById::get)
				.filter(Objects::nonNull)
				.map(comment -> CommentMapper.toResponse(
						comment, mentionsByCommentId.getOrDefault(comment.getId(), List.of())))
				.toList();

		return MentionPageResponse.builder()
				.data(data)
				.total(commentIdPage.getTotalElements())
				.page(resolvedPage)
				.build();
	}

	private void requireUser(Long userId) {
		userRepository
				.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
	}
}
