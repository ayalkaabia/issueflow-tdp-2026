package com.att.tdp.issueflow.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MentionPageResponse {

	private List<CommentResponse> data;
	private long total;
	private int page;
}
