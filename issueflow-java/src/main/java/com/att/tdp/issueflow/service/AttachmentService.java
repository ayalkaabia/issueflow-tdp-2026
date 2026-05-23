package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.config.AttachmentStorageProperties;
import com.att.tdp.issueflow.dto.response.AttachmentResponse;
import com.att.tdp.issueflow.exception.BusinessRuleException;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.mapper.AttachmentMapper;
import com.att.tdp.issueflow.model.entity.Attachment;
import com.att.tdp.issueflow.model.enums.AuditAction;
import com.att.tdp.issueflow.model.enums.AuditActor;
import com.att.tdp.issueflow.model.enums.AuditEntityType;
import com.att.tdp.issueflow.repository.AttachmentRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AttachmentService {

	public static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

	public static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
			"image/png",
			"image/jpeg",
			"image/jpg",
			"application/pdf",
			"text/plain");

	private static final Map<String, String> CONTENT_TYPE_EXTENSIONS = Map.of(
			"image/png", ".png",
			"image/jpeg", ".jpg",
			"image/jpg", ".jpg",
			"application/pdf", ".pdf",
			"text/plain", ".txt");

	private final AttachmentRepository attachmentRepository;
	private final TicketRepository ticketRepository;
	private final AttachmentStorageProperties storageProperties;
	private final AuditService auditService;

	@Transactional
	public AttachmentResponse uploadAttachment(Long ticketId, MultipartFile file, Long performedBy) {
		requireActiveTicket(ticketId);
		validateFile(file);

		String contentType = normalizeContentType(file.getContentType());
		String storedFileName = UUID.randomUUID() + CONTENT_TYPE_EXTENSIONS.get(contentType);
		String storagePath = ticketId + "/" + storedFileName;

		Path destination = resolveUploadRoot().resolve(storagePath);
		try {
			Files.createDirectories(destination.getParent());
			Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ex) {
			throw new BusinessRuleException("Failed to store attachment: " + ex.getMessage());
		}

		Attachment attachment = new Attachment();
		attachment.setTicketId(ticketId);
		attachment.setOriginalFileName(sanitizeOriginalFileName(file.getOriginalFilename()));
		attachment.setStoredFileName(storedFileName);
		attachment.setContentType(contentType);
		attachment.setSizeBytes(file.getSize());
		attachment.setStoragePath(storagePath);

		Attachment saved = attachmentRepository.save(attachment);
		auditService.log(
				AuditAction.CREATE, AuditEntityType.ATTACHMENT, saved.getId(), performedBy, AuditActor.USER);
		return AttachmentMapper.toResponse(saved);
	}

	@Transactional
	public void deleteAttachment(Long ticketId, Long attachmentId, Long performedBy) {
		requireActiveTicket(ticketId);

		Attachment attachment = attachmentRepository
				.findByIdAndTicketId(attachmentId, ticketId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Attachment not found for ticket " + ticketId + ": " + attachmentId));

		deleteStoredFile(attachment.getStoragePath());
		attachmentRepository.delete(attachment);
		auditService.log(
				AuditAction.DELETE, AuditEntityType.ATTACHMENT, attachmentId, performedBy, AuditActor.USER);
	}

	private void validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BusinessRuleException("Attachment file is required");
		}
		if (file.getSize() > MAX_FILE_SIZE_BYTES) {
			throw new BusinessRuleException("Attachment exceeds maximum size of 10 MB");
		}
		String contentType = normalizeContentType(file.getContentType());
		if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
			throw new BusinessRuleException(
					"Unsupported file type. Allowed types: image/png, image/jpeg, application/pdf, text/plain");
		}
	}

	private String normalizeContentType(String contentType) {
		if (!StringUtils.hasText(contentType)) {
			throw new BusinessRuleException("File content type is required");
		}
		return contentType.trim().toLowerCase();
	}

	private String sanitizeOriginalFileName(String originalFilename) {
		if (!StringUtils.hasText(originalFilename)) {
			return "attachment";
		}
		String name = Paths.get(originalFilename).getFileName().toString();
		if (name.contains("..") || name.contains("/") || name.contains("\\")) {
			return "attachment";
		}
		return name;
	}

	private void requireActiveTicket(Long ticketId) {
		ticketRepository
				.findByIdAndDeletedAtIsNull(ticketId)
				.orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));
	}

	private Path resolveUploadRoot() {
		return Paths.get(storageProperties.getUploadDir()).toAbsolutePath().normalize();
	}

	private void deleteStoredFile(String storagePath) {
		try {
			Path filePath = resolveUploadRoot().resolve(storagePath).normalize();
			if (!filePath.startsWith(resolveUploadRoot())) {
				throw new BusinessRuleException("Invalid attachment storage path");
			}
			Files.deleteIfExists(filePath);
		} catch (IOException ex) {
			throw new BusinessRuleException("Failed to delete attachment file: " + ex.getMessage());
		}
	}
}
