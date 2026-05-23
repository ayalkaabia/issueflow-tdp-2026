package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.request.AddDependencyRequest;
import com.att.tdp.issueflow.dto.response.DependencyTicketResponse;
import com.att.tdp.issueflow.exception.BusinessRuleException;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.model.entity.Ticket;
import com.att.tdp.issueflow.model.entity.TicketDependency;
import com.att.tdp.issueflow.model.enums.AuditAction;
import com.att.tdp.issueflow.model.enums.AuditActor;
import com.att.tdp.issueflow.model.enums.AuditEntityType;
import com.att.tdp.issueflow.repository.TicketDependencyRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketDependencyService {

	private final TicketDependencyRepository ticketDependencyRepository;
	private final TicketRepository ticketRepository;
	private final AuditService auditService;

	@Transactional
	public void addDependency(Long ticketId, AddDependencyRequest request, Long performedBy) {
		Long blockedById = request.getBlockedBy();

		if (Objects.equals(ticketId, blockedById)) {
			throw new BusinessRuleException("A ticket cannot depend on itself");
		}

		Ticket ticket = requireActiveTicket(ticketId);
		Ticket blocker = requireActiveTicket(blockedById);

		if (!Objects.equals(ticket.getProjectId(), blocker.getProjectId())) {
			throw new BusinessRuleException("Both tickets must belong to the same project");
		}

		if (ticketDependencyRepository.existsByTicketIdAndBlockedById(ticketId, blockedById)) {
			throw new BusinessRuleException("Dependency already exists");
		}

		TicketDependency dependency = new TicketDependency();
		dependency.setTicketId(ticketId);
		dependency.setBlockedById(blockedById);

		TicketDependency saved = ticketDependencyRepository.save(dependency);
		auditService.log(
				AuditAction.CREATE, AuditEntityType.DEPENDENCY, saved.getId(), performedBy, AuditActor.USER);
	}

	@Transactional(readOnly = true)
	public List<DependencyTicketResponse> listDependencies(Long ticketId) {
		requireActiveTicket(ticketId);

		return ticketDependencyRepository.findByTicketId(ticketId).stream()
				.map(dependency -> ticketRepository.findByIdAndDeletedAtIsNull(dependency.getBlockedById()))
				.flatMap(java.util.Optional::stream)
				.map(blocker -> DependencyTicketResponse.builder()
						.id(blocker.getId())
						.title(blocker.getTitle())
						.status(blocker.getStatus())
						.build())
				.toList();
	}

	@Transactional
	public void removeDependency(Long ticketId, Long blockerId, Long performedBy) {
		requireActiveTicket(ticketId);

		TicketDependency dependency = ticketDependencyRepository
				.findByTicketIdAndBlockedById(ticketId, blockerId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Dependency not found for ticket " + ticketId + " blocked by " + blockerId));

		ticketDependencyRepository.delete(dependency);
		auditService.log(
				AuditAction.DELETE, AuditEntityType.DEPENDENCY, dependency.getId(), performedBy, AuditActor.USER);
	}

	private Ticket requireActiveTicket(Long ticketId) {
		return ticketRepository
				.findByIdAndDeletedAtIsNull(ticketId)
				.orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));
	}
}
