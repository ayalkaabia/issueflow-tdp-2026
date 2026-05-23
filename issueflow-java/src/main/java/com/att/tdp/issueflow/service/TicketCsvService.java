package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.request.CreateTicketRequest;
import com.att.tdp.issueflow.dto.response.TicketImportResponse;
import com.att.tdp.issueflow.exception.BusinessRuleException;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.model.entity.Ticket;
import com.att.tdp.issueflow.model.enums.AuditAction;
import com.att.tdp.issueflow.model.enums.AuditActor;
import com.att.tdp.issueflow.model.enums.AuditEntityType;
import com.att.tdp.issueflow.model.enums.TicketPriority;
import com.att.tdp.issueflow.model.enums.TicketStatus;
import com.att.tdp.issueflow.model.enums.TicketType;
import com.att.tdp.issueflow.repository.ProjectRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class TicketCsvService {

	public static final String[] EXPORT_HEADERS = {
		"id", "title", "description", "status", "priority", "type", "assigneeId"
	};

	private static final Set<String> REQUIRED_IMPORT_HEADERS = Set.of("title", "priority", "type");

	private final TicketRepository ticketRepository;
	private final ProjectRepository projectRepository;
	private final TicketService ticketService;
	private final AuditService auditService;

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

	@Transactional
	public TicketImportResponse importTickets(Long projectId, MultipartFile file, Long performedBy) {
		requireActiveProject(projectId);
		validateCsvFile(file);

		List<String> errors = new ArrayList<>();
		int created = 0;
		int failed = 0;

		CSVFormat format = CSVFormat.DEFAULT.builder()
				.setHeader()
				.setSkipHeaderRecord(true)
				.setIgnoreHeaderCase(true)
				.setTrim(true)
				.build();

		try (CSVParser parser =
				new CSVParser(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8), format)) {
			validateImportHeaders(parser.getHeaderMap().keySet());

			for (CSVRecord record : parser) {
				int rowNumber = (int) record.getRecordNumber() + 1;
				try {
					CreateTicketRequest request = toCreateRequest(record, projectId);
					ticketService.createTicket(request, performedBy);
					created++;
				} catch (RuntimeException ex) {
					failed++;
					errors.add("Row " + rowNumber + ": " + ex.getMessage());
				}
			}
		} catch (IOException ex) {
			throw new BusinessRuleException("Failed to read CSV file: " + ex.getMessage());
		}

		if (created > 0) {
			auditService.log(
					AuditAction.IMPORT, AuditEntityType.TICKET, projectId, performedBy, AuditActor.USER);
		}

		return TicketImportResponse.builder()
				.created(created)
				.failed(failed)
				.errors(errors)
				.build();
	}

	private CreateTicketRequest toCreateRequest(CSVRecord record, Long projectId) {
		String title = requiredValue(record, "title");
		TicketPriority priority = parseEnum(record, "priority", TicketPriority.class);
		TicketType type = parseEnum(record, "type", TicketType.class);
		TicketStatus status = parseOptionalEnum(record, "status", TicketStatus.class, TicketStatus.TODO);

		if (status != TicketStatus.TODO) {
			throw new BusinessRuleException("Imported tickets must start with status TODO");
		}

		CreateTicketRequest request = new CreateTicketRequest();
		request.setTitle(title);
		request.setDescription(emptyToNull(record, "description"));
		request.setPriority(priority);
		request.setType(type);
		request.setStatus(status);
		request.setProjectId(projectId);
		request.setAssigneeId(parseOptionalLong(record, "assigneeId"));
		return request;
	}

	private void validateCsvFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BusinessRuleException("CSV file is required");
		}
		String filename = file.getOriginalFilename();
		boolean csvName = filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".csv");
		String contentType = file.getContentType();
		boolean csvType = contentType != null
				&& (contentType.equalsIgnoreCase("text/csv")
						|| contentType.equalsIgnoreCase("application/csv")
						|| contentType.equalsIgnoreCase("application/vnd.ms-excel"));
		if (!csvName && !csvType) {
			throw new BusinessRuleException("Only CSV files are supported for import");
		}
	}

	private void validateImportHeaders(Set<String> headers) {
		for (String required : REQUIRED_IMPORT_HEADERS) {
			if (!headers.contains(required)) {
				throw new BusinessRuleException("CSV header must include: " + String.join(", ", EXPORT_HEADERS));
			}
		}
	}

	private String requiredValue(CSVRecord record, String column) {
		String value = emptyToNull(record, column);
		if (!StringUtils.hasText(value)) {
			throw new BusinessRuleException("Missing required field '" + column + "'");
		}
		return value;
	}

	private String emptyToNull(CSVRecord record, String column) {
		if (!record.isMapped(column)) {
			return null;
		}
		String value = record.get(column);
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private Long parseOptionalLong(CSVRecord record, String column) {
		String value = emptyToNull(record, column);
		if (value == null) {
			return null;
		}
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException ex) {
			throw new BusinessRuleException("Invalid numeric value for '" + column + "'");
		}
	}

	private <E extends Enum<E>> E parseEnum(CSVRecord record, String column, Class<E> enumType) {
		String value = requiredValue(record, column);
		try {
			return Enum.valueOf(enumType, value.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			throw new BusinessRuleException("Invalid value for '" + column + "': " + value);
		}
	}

	private <E extends Enum<E>> E parseOptionalEnum(
			CSVRecord record, String column, Class<E> enumType, E defaultValue) {
		String value = emptyToNull(record, column);
		if (value == null) {
			return defaultValue;
		}
		try {
			return Enum.valueOf(enumType, value.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			throw new BusinessRuleException("Invalid value for '" + column + "': " + value);
		}
	}

	private void requireActiveProject(Long projectId) {
		projectRepository
				.findByIdAndDeletedAtIsNull(projectId)
				.orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
	}
}
