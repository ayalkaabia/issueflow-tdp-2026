package com.att.tdp.issueflow.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WorkloadResponse {

	private Long userId;
	private String username;
	private long openTicketCount;
}
