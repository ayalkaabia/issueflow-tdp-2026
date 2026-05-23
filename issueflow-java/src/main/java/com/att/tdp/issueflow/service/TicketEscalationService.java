package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.model.entity.Ticket;
import com.att.tdp.issueflow.model.enums.AuditAction;
import com.att.tdp.issueflow.model.enums.AuditActor;
import com.att.tdp.issueflow.model.enums.AuditEntityType;
import com.att.tdp.issueflow.model.enums.TicketPriority;
import com.att.tdp.issueflow.model.enums.TicketStatus;
import com.att.tdp.issueflow.repository.TicketRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketEscalationService {

	private final TicketRepository ticketRepository;
	private final AuditService auditService;

	@Scheduled(fixedDelayString = "${issueflow.escalation.fixed-delay-ms:60000}")
	@Transactional
	public void escalateOverdueTickets() {
		List<Ticket> overdueTickets = ticketRepository.findByDeletedAtIsNullAndDueDateBeforeAndStatusNot(
				Instant.now(), TicketStatus.DONE);

		for (Ticket ticket : overdueTickets) {
			ticket.setOverdue(true);

			TicketPriority escalatedPriority = escalatePriority(ticket.getPriority());
			if (escalatedPriority != ticket.getPriority()) {
				ticket.setPriority(escalatedPriority);
				ticketRepository.save(ticket);
				auditService.log(
						AuditAction.ESCALATE,
						AuditEntityType.TICKET,
						ticket.getId(),
						null,
						AuditActor.SYSTEM);
			} else {
				ticketRepository.save(ticket);
			}
		}
	}

	static TicketPriority escalatePriority(TicketPriority current) {
		return switch (current) {
			case LOW -> TicketPriority.MEDIUM;
			case MEDIUM -> TicketPriority.HIGH;
			case HIGH, CRITICAL -> TicketPriority.CRITICAL;
		};
	}
}
