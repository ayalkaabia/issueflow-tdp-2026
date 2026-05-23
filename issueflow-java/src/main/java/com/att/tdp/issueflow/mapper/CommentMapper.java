package com.att.tdp.issueflow.mapper;

import com.att.tdp.issueflow.dto.response.CommentResponse;
import com.att.tdp.issueflow.dto.response.MentionedUserResponse;
import com.att.tdp.issueflow.model.entity.Comment;
import java.util.List;

public final class CommentMapper {

	private CommentMapper() {}

	public static CommentResponse toResponse(Comment comment, List<MentionedUserResponse> mentionedUsers) {
		return CommentResponse.builder()
				.id(comment.getId())
				.ticketId(comment.getTicketId())
				.authorId(comment.getAuthorId())
				.content(comment.getContent())
				.version(comment.getVersion())
				.mentionedUsers(mentionedUsers)
				.build();
	}
}
