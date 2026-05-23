package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.model.entity.Attachment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

	List<Attachment> findByTicketId(Long ticketId);

	Optional<Attachment> findByIdAndTicketId(Long id, Long ticketId);
}
