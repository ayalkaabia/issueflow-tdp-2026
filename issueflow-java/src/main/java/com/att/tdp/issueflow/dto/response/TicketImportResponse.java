package com.att.tdp.issueflow.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TicketImportResponse {

	private int created;
	private int failed;
	private List<String> errors;
}
