package com.att.tdp.issueflow.dto.response;

import com.att.tdp.issueflow.model.enums.TicketPriority;
import com.att.tdp.issueflow.model.enums.TicketStatus;
import com.att.tdp.issueflow.model.enums.TicketType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TicketResponse {

	private Long id;
	private String title;
	private String description;
	private TicketStatus status;
	private TicketPriority priority;
	private TicketType type;
	private Long projectId;
	private Long assigneeId;
	private Instant dueDate;

	@JsonProperty("isOverdue")
	private boolean overdue;

	private Long version;
}
