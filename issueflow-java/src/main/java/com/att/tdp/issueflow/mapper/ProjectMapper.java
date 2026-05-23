package com.att.tdp.issueflow.mapper;

import com.att.tdp.issueflow.dto.response.ProjectResponse;
import com.att.tdp.issueflow.model.entity.Project;

public final class ProjectMapper {

	private ProjectMapper() {}

	public static ProjectResponse toResponse(Project project) {
		return ProjectResponse.builder()
				.id(project.getId())
				.name(project.getName())
				.description(project.getDescription())
				.ownerId(project.getOwnerId())
				.build();
	}
}
