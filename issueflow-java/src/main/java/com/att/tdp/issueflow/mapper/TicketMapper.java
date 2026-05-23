package com.att.tdp.issueflow.mapper;

import com.att.tdp.issueflow.dto.response.TicketResponse;
import com.att.tdp.issueflow.model.entity.Ticket;

public final class TicketMapper {

	private TicketMapper() {}

	public static TicketResponse toResponse(Ticket ticket) {
		return TicketResponse.builder()
				.id(ticket.getId())
				.title(ticket.getTitle())
				.description(ticket.getDescription())
				.status(ticket.getStatus())
				.priority(ticket.getPriority())
				.type(ticket.getType())
				.projectId(ticket.getProjectId())
				.assigneeId(ticket.getAssigneeId())
				.dueDate(ticket.getDueDate())
				.overdue(ticket.isOverdue())
				.version(ticket.getVersion())
				.build();
	}
}
