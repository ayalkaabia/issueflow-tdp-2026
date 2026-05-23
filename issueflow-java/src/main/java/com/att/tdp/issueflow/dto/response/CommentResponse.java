package com.att.tdp.issueflow.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentResponse {

	private Long id;
	private Long ticketId;
	private Long authorId;
	private String content;
	private Long version;
	private List<MentionedUserResponse> mentionedUsers;
}
