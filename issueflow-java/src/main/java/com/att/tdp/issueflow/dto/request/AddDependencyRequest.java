package com.att.tdp.issueflow.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddDependencyRequest {

	@NotNull
	private Long blockedBy;
}
