package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.response.MentionedUserResponse;
import com.att.tdp.issueflow.exception.BusinessRuleException;
import com.att.tdp.issueflow.model.entity.CommentMention;
import com.att.tdp.issueflow.model.entity.User;
import com.att.tdp.issueflow.repository.CommentMentionRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentMentionService {

	private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

	private final CommentMentionRepository commentMentionRepository;
	private final UserRepository userRepository;

	public List<String> extractUsernamesInOrder(String content) {
		if (content == null || content.isBlank()) {
			return List.of();
		}
		LinkedHashSet<String> usernames = new LinkedHashSet<>();
		Matcher matcher = MENTION_PATTERN.matcher(content);
		while (matcher.find()) {
			usernames.add(matcher.group(1));
		}
		return List.copyOf(usernames);
	}

	public List<User> resolveMentionedUsers(String content) {
		List<String> usernames = extractUsernamesInOrder(content);
		if (usernames.isEmpty()) {
			return List.of();
		}

		Map<String, User> usersByUsername =
				userRepository.findByUsernameIn(usernames).stream()
						.collect(Collectors.toMap(User::getUsername, user -> user));

		List<User> resolved = new ArrayList<>();
		for (String username : usernames) {
			User user = usersByUsername.get(username);
			if (user == null) {
				throw new BusinessRuleException("Unknown user mention: @" + username);
			}
			resolved.add(user);
		}
		return resolved;
	}

	@Transactional
	public void syncMentions(Long commentId, String content) {
		commentMentionRepository.deleteByCommentId(commentId);
		for (User user : resolveMentionedUsers(content)) {
			CommentMention mention = new CommentMention();
			mention.setCommentId(commentId);
			mention.setMentionedUserId(user.getId());
			commentMentionRepository.save(mention);
		}
	}

	@Transactional(readOnly = true)
	public List<MentionedUserResponse> getMentionedUsers(Long commentId) {
		return getMentionedUsersByCommentIds(List.of(commentId))
				.getOrDefault(commentId, List.of());
	}

	@Transactional(readOnly = true)
	public Map<Long, List<MentionedUserResponse>> getMentionedUsersByCommentIds(Collection<Long> commentIds) {
		if (commentIds == null || commentIds.isEmpty()) {
			return Map.of();
		}

		List<CommentMention> mentions =
				commentMentionRepository.findByCommentIdInOrderByIdAsc(commentIds);
		if (mentions.isEmpty()) {
			return Map.of();
		}

		Set<Long> userIds = mentions.stream()
				.map(CommentMention::getMentionedUserId)
				.collect(Collectors.toSet());
		Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
				.collect(Collectors.toMap(User::getId, user -> user));

		Map<Long, List<MentionedUserResponse>> result = new HashMap<>();
		for (CommentMention mention : mentions) {
			User user = usersById.get(mention.getMentionedUserId());
			if (user != null) {
				result.computeIfAbsent(mention.getCommentId(), ignored -> new ArrayList<>())
						.add(MentionedUserResponse.from(user));
			}
		}
		return result;
	}
}
