package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.exception.BusinessRuleException;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.model.entity.Ticket;
import com.att.tdp.issueflow.repository.ProjectRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketCsvService {

	public static final String[] EXPORT_HEADERS = {
		"id", "title", "description", "status", "priority", "type", "assigneeId"
	};

	private final TicketRepository ticketRepository;
	private final ProjectRepository projectRepository;

	@Transactional(readOnly = true)
	public byte[] exportTickets(Long projectId) {
		requireActiveProject(projectId);
		List<Ticket> tickets = ticketRepository.findByProjectIdAndDeletedAtIsNull(projectId);

		try (ByteArrayOutputStream output = new ByteArrayOutputStream();
				OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
				CSVPrinter printer =
						new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(EXPORT_HEADERS).build())) {
			for (Ticket ticket : tickets) {
				printer.printRecord(
						ticket.getId(),
						ticket.getTitle(),
						ticket.getDescription(),
						ticket.getStatus(),
						ticket.getPriority(),
						ticket.getType(),
						ticket.getAssigneeId());
			}
			printer.flush();
			return output.toByteArray();
		} catch (IOException ex) {
			throw new BusinessRuleException("Failed to export tickets: " + ex.getMessage());
		}
	}

	private void requireActiveProject(Long projectId) {
		projectRepository
				.findByIdAndDeletedAtIsNull(projectId)
				.orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
	}
}
