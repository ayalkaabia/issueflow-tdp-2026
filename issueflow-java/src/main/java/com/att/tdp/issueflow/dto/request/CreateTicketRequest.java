package com.att.tdp.issueflow.dto.request;

import com.att.tdp.issueflow.model.enums.TicketPriority;
import com.att.tdp.issueflow.model.enums.TicketStatus;
import com.att.tdp.issueflow.model.enums.TicketType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTicketRequest {

	@NotBlank
	private String title;

	private String description;

	private TicketStatus status;

	@NotNull
	private TicketPriority priority;

	@NotNull
	private TicketType type;

	@NotNull
	private Long projectId;

	private Long assigneeId;

	private Instant dueDate;
}
