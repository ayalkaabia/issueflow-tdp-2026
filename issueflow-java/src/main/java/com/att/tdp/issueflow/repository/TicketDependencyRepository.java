package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.model.entity.TicketDependency;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketDependencyRepository extends JpaRepository<TicketDependency, Long> {

	@Query(
			"""
			SELECT CASE WHEN COUNT(td) > 0 THEN true ELSE false END
			FROM TicketDependency td
			JOIN Ticket blocker ON blocker.id = td.blockedById
			WHERE td.ticketId = :ticketId
			  AND blocker.deletedAt IS NULL
			  AND blocker.status <> com.att.tdp.issueflow.model.enums.TicketStatus.DONE
			""")
	boolean hasUnresolvedBlockers(@Param("ticketId") Long ticketId);

	List<TicketDependency> findByTicketId(Long ticketId);

	Optional<TicketDependency> findByTicketIdAndBlockedById(Long ticketId, Long blockedById);

	boolean existsByTicketIdAndBlockedById(Long ticketId, Long blockedById);

	void deleteByTicketIdAndBlockedById(Long ticketId, Long blockedById);
}
