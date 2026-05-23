package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.request.CreateProjectRequest;
import com.att.tdp.issueflow.dto.request.UpdateProjectRequest;
import com.att.tdp.issueflow.dto.response.ProjectResponse;
import com.att.tdp.issueflow.exception.BusinessRuleException;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.mapper.ProjectMapper;
import com.att.tdp.issueflow.model.entity.Project;
import com.att.tdp.issueflow.model.enums.AuditAction;
import com.att.tdp.issueflow.model.enums.AuditActor;
import com.att.tdp.issueflow.model.enums.AuditEntityType;
import com.att.tdp.issueflow.model.enums.Role;
import com.att.tdp.issueflow.repository.ProjectRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProjectService {

	private final ProjectRepository projectRepository;
	private final UserRepository userRepository;
	private final AuditService auditService;

	@Transactional(readOnly = true)
	public List<ProjectResponse> getAllProjects() {
		return projectRepository.findByDeletedAtIsNull().stream()
				.map(ProjectMapper::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public ProjectResponse getProjectById(Long projectId) {
		return ProjectMapper.toResponse(requireActiveProject(projectId));
	}

	@Transactional
	public ProjectResponse createProject(CreateProjectRequest request, Long performedBy) {
		requireUser(request.getOwnerId());

		Project project = new Project();
		project.setName(request.getName());
		project.setDescription(request.getDescription());
		project.setOwnerId(request.getOwnerId());

		Project saved = projectRepository.save(project);
		auditService.log(
				AuditAction.CREATE, AuditEntityType.PROJECT, saved.getId(), performedBy, AuditActor.USER);
		return ProjectMapper.toResponse(saved);
	}

	@Transactional
	public ProjectResponse updateProject(Long projectId, UpdateProjectRequest request, Long performedBy) {
		Project project = requireActiveProject(projectId);

		if (!StringUtils.hasText(request.getName()) && request.getDescription() == null) {
			throw new BusinessRuleException("At least one of name or description must be provided");
		}

		if (StringUtils.hasText(request.getName())) {
			project.setName(request.getName());
		}
		if (request.getDescription() != null) {
			project.setDescription(request.getDescription());
		}

		Project saved = projectRepository.save(project);
		auditService.log(
				AuditAction.UPDATE, AuditEntityType.PROJECT, saved.getId(), performedBy, AuditActor.USER);
		return ProjectMapper.toResponse(saved);
	}

	@Transactional
	public void softDeleteProject(Long projectId, Long performedBy) {
		Project project = requireActiveProject(projectId);
		project.setDeletedAt(Instant.now());
		projectRepository.save(project);
		auditService.log(
				AuditAction.DELETE, AuditEntityType.PROJECT, projectId, performedBy, AuditActor.USER);
	}

	@Transactional(readOnly = true)
	public List<ProjectResponse> getDeletedProjects(Long performedBy) {
		requireAdmin(performedBy);
		return projectRepository.findByDeletedAtIsNotNull().stream()
				.map(ProjectMapper::toResponse)
				.toList();
	}

	@Transactional
	public void restoreProject(Long projectId, Long performedBy) {
		requireAdmin(performedBy);

		Project project = projectRepository
				.findByIdAndDeletedAtIsNotNull(projectId)
				.orElseThrow(() -> new ResourceNotFoundException("Deleted project not found: " + projectId));

		project.setDeletedAt(null);
		Project saved = projectRepository.save(project);
		auditService.log(
				AuditAction.RESTORE, AuditEntityType.PROJECT, saved.getId(), performedBy, AuditActor.USER);
	}

	private Project requireActiveProject(Long projectId) {
		return projectRepository
				.findByIdAndDeletedAtIsNull(projectId)
				.orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
	}

	private void requireUser(Long userId) {
		userRepository
				.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
	}

	private void requireAdmin(Long performedBy) {
		var user = userRepository
				.findById(performedBy)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + performedBy));
		if (user.getRole() != Role.ADMIN) {
			throw new BusinessRuleException(
					"Only ADMIN users can access deleted projects or restore them");
		}
	}
}
