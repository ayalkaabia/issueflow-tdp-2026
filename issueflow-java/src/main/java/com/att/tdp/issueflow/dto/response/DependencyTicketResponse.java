package com.att.tdp.issueflow.dto.response;

import com.att.tdp.issueflow.model.enums.TicketStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DependencyTicketResponse {

	private Long id;
	private String title;
	private TicketStatus status;
}
