package com.att.tdp.issueflow.dto.request;

import com.att.tdp.issueflow.model.enums.TicketPriority;
import com.att.tdp.issueflow.model.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTicketRequest {

	private String title;

	private String description;

	private TicketStatus status;

	private TicketPriority priority;

	private Long assigneeId;

	private Instant dueDate;

	@NotNull
	private Long version;
}
