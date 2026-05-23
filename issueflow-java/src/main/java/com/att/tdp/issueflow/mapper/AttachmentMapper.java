package com.att.tdp.issueflow.mapper;

import com.att.tdp.issueflow.dto.response.AttachmentResponse;
import com.att.tdp.issueflow.model.entity.Attachment;

public final class AttachmentMapper {

	private AttachmentMapper() {}

	public static AttachmentResponse toResponse(Attachment attachment) {
		return AttachmentResponse.builder()
				.id(attachment.getId())
				.ticketId(attachment.getTicketId())
				.filename(attachment.getOriginalFileName())
				.contentType(attachment.getContentType())
				.build();
	}
}
