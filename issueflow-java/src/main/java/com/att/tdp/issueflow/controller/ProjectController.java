package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.request.CreateProjectRequest;
import com.att.tdp.issueflow.dto.request.UpdateProjectRequest;
import com.att.tdp.issueflow.dto.response.ProjectResponse;
import com.att.tdp.issueflow.security.SecurityUtils;
import com.att.tdp.issueflow.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/projects")
@Validated
@RequiredArgsConstructor
public class ProjectController {

	private final ProjectService projectService;

	@GetMapping
	public List<ProjectResponse> getAllProjects() {
		return projectService.getAllProjects();
	}

	@GetMapping("/deleted")
	public List<ProjectResponse> getDeletedProjects() {
		return projectService.getDeletedProjects(SecurityUtils.getCurrentUserId());
	}

	@GetMapping("/{projectId}")
	public ProjectResponse getProjectById(@PathVariable Long projectId) {
		return projectService.getProjectById(projectId);
	}

	@PostMapping
	public ProjectResponse createProject(@Valid @RequestBody CreateProjectRequest request) {
		return projectService.createProject(request, SecurityUtils.getCurrentUserId());
	}

	@PatchMapping("/{projectId}")
	public ProjectResponse updateProject(
			@PathVariable Long projectId, @Valid @RequestBody UpdateProjectRequest request) {
		return projectService.updateProject(projectId, request, SecurityUtils.getCurrentUserId());
	}

	@DeleteMapping("/{projectId}")
	public ResponseEntity<Void> softDeleteProject(@PathVariable Long projectId) {
		projectService.softDeleteProject(projectId, SecurityUtils.getCurrentUserId());
		return ResponseEntity.ok().build();
	}

	@PostMapping("/{projectId}/restore")
	public ResponseEntity<Void> restoreProject(@PathVariable Long projectId) {
		projectService.restoreProject(projectId, SecurityUtils.getCurrentUserId());
		return ResponseEntity.ok().build();
	}
}
