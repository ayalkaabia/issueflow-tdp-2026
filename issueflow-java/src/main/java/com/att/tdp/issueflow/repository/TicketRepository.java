package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.model.entity.Ticket;
import com.att.tdp.issueflow.model.enums.TicketStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

	List<Ticket> findByProjectIdAndDeletedAtIsNull(Long projectId);

	List<Ticket> findByProjectIdAndDeletedAtIsNotNull(Long projectId);

	Optional<Ticket> findByIdAndDeletedAtIsNull(Long id);

	Optional<Ticket> findByIdAndDeletedAtIsNotNull(Long id);

	long countByProjectIdAndAssigneeIdAndDeletedAtIsNullAndStatusIn(
			Long projectId, Long assigneeId, Collection<TicketStatus> statuses);

	List<Ticket> findByDeletedAtIsNullAndDueDateBeforeAndStatusNot(Instant dueDate, TicketStatus status);
}
