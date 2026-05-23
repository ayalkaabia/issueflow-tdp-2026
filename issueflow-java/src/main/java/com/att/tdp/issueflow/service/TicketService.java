package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.request.CreateTicketRequest;
import com.att.tdp.issueflow.dto.request.UpdateTicketRequest;
import com.att.tdp.issueflow.dto.response.TicketResponse;
import com.att.tdp.issueflow.exception.BusinessRuleException;
import com.att.tdp.issueflow.exception.OptimisticLockConflictException;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.mapper.TicketMapper;
import com.att.tdp.issueflow.model.entity.Ticket;
import com.att.tdp.issueflow.model.entity.User;
import com.att.tdp.issueflow.model.enums.AuditAction;
import com.att.tdp.issueflow.model.enums.AuditActor;
import com.att.tdp.issueflow.model.enums.AuditEntityType;
import com.att.tdp.issueflow.model.enums.Role;
import com.att.tdp.issueflow.model.enums.TicketStatus;
import com.att.tdp.issueflow.repository.ProjectRepository;
import com.att.tdp.issueflow.repository.TicketDependencyRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketService {

	private static final Set<TicketStatus> OPEN_STATUSES =
			EnumSet.of(TicketStatus.TODO, TicketStatus.IN_PROGRESS, TicketStatus.IN_REVIEW);

	private final TicketRepository ticketRepository;
	private final ProjectRepository projectRepository;
	private final UserRepository userRepository;
	private final TicketDependencyRepository ticketDependencyRepository;
	private final AuditService auditService;

	@Transactional
	public TicketResponse createTicket(CreateTicketRequest request, Long performedBy) {
		requireActiveProject(request.getProjectId());
		if (request.getAssigneeId() != null) {
			requireUser(request.getAssigneeId());
		}

		Ticket ticket = new Ticket();
		ticket.setTitle(request.getTitle());
		ticket.setDescription(request.getDescription());
		ticket.setPriority(request.getPriority());
		ticket.setType(request.getType());
		ticket.setProjectId(request.getProjectId());
		ticket.setAssigneeId(request.getAssigneeId());
		ticket.setDueDate(request.getDueDate());

		TicketStatus status = request.getStatus() != null ? request.getStatus() : TicketStatus.TODO;
		if (status != TicketStatus.TODO) {
			throw new BusinessRuleException("New tickets must start with status TODO");
		}
		ticket.setStatus(status);
		ticket.setOverdue(computeOverdueFlag(request.getDueDate()));

		boolean autoAssigned = false;
		if (ticket.getAssigneeId() == null) {
			Long developerId = resolveLeastLoadedDeveloper(request.getProjectId());
			if (developerId != null) {
				ticket.setAssigneeId(developerId);
				autoAssigned = true;
			}
		}

		Ticket saved = ticketRepository.save(ticket);
		auditService.log(
				AuditAction.CREATE, AuditEntityType.TICKET, saved.getId(), performedBy, AuditActor.USER);
		if (autoAssigned) {
			auditService.log(
					AuditAction.AUTO_ASSIGN,
					AuditEntityType.TICKET,
					saved.getId(),
					performedBy,
					AuditActor.SYSTEM);
		}
		return TicketMapper.toResponse(saved);
	}

	@Transactional(readOnly = true)
	public TicketResponse getTicketById(Long ticketId) {
		return TicketMapper.toResponse(requireActiveTicket(ticketId));
	}

	@Transactional(readOnly = true)
	public List<TicketResponse> getTicketsByProject(Long projectId) {
		requireActiveProject(projectId);
		return ticketRepository.findByProjectIdAndDeletedAtIsNull(projectId).stream()
				.map(TicketMapper::toResponse)
				.toList();
	}

	@Transactional
	public TicketResponse updateTicket(Long ticketId, UpdateTicketRequest request, Long performedBy) {
		Ticket ticket = requireActiveTicket(ticketId);

		if (ticket.getStatus() == TicketStatus.DONE) {
			throw new BusinessRuleException("DONE tickets cannot be updated");
		}

		if (!Objects.equals(ticket.getVersion(), request.getVersion())) {
			throw new OptimisticLockConflictException(
					"Ticket was modified by another user; refresh and retry with the current version");
		}

		if (request.getTitle() != null) {
			ticket.setTitle(request.getTitle());
		}
		if (request.getDescription() != null) {
			ticket.setDescription(request.getDescription());
		}
		if (request.getDueDate() != null) {
			ticket.setDueDate(request.getDueDate());
			ticket.setOverdue(computeOverdueFlag(request.getDueDate()));
		}

		if (request.getAssigneeId() != null) {
			requireUser(request.getAssigneeId());
			ticket.setAssigneeId(request.getAssigneeId());
		}

		if (request.getPriority() != null && request.getPriority() != ticket.getPriority()) {
			ticket.setPriority(request.getPriority());
			ticket.setOverdue(false);
		}

		if (request.getStatus() != null && request.getStatus() != ticket.getStatus()) {
			validateStatusTransition(ticket, request.getStatus());
			ticket.setStatus(request.getStatus());
		}

		try {
			Ticket saved = ticketRepository.saveAndFlush(ticket);
			auditService.log(
					AuditAction.UPDATE, AuditEntityType.TICKET, saved.getId(), performedBy, AuditActor.USER);
			return TicketMapper.toResponse(saved);
		} catch (OptimisticLockingFailureException ex) {
			throw new OptimisticLockConflictException(
					"Ticket was modified by another user; refresh and retry with the current version", ex);
		}
	}

	private void validateStatusTransition(Ticket ticket, TicketStatus targetStatus) {
		if (!ticket.getStatus().canTransitionTo(targetStatus)) {
			throw new BusinessRuleException(
					"Invalid status transition from " + ticket.getStatus() + " to " + targetStatus);
		}
		if (targetStatus == TicketStatus.DONE
				&& ticketDependencyRepository.hasUnresolvedBlockers(ticket.getId())) {
			throw new BusinessRuleException("Cannot move ticket to DONE while unresolved blockers exist");
		}
	}

	private Long resolveLeastLoadedDeveloper(Long projectId) {
		List<User> developers = userRepository.findByRole(Role.DEVELOPER);
		if (developers.isEmpty()) {
			return null;
		}

		return developers.stream()
				.min(Comparator.comparingLong(
								(User dev) -> ticketRepository.countByProjectIdAndAssigneeIdAndDeletedAtIsNullAndStatusIn(
										projectId, dev.getId(), OPEN_STATUSES))
						.thenComparingLong(User::getId))
				.map(User::getId)
				.orElse(null);
	}

	private boolean computeOverdueFlag(Instant dueDate) {
		return dueDate != null && dueDate.isBefore(Instant.now());
	}

	private Ticket requireActiveTicket(Long ticketId) {
		return ticketRepository
				.findByIdAndDeletedAtIsNull(ticketId)
				.orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));
	}

	private void requireActiveProject(Long projectId) {
		projectRepository
				.findByIdAndDeletedAtIsNull(projectId)
				.orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
	}

	private void requireUser(Long userId) {
		userRepository
				.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
	}
}
