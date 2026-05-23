package com.att.tdp.issueflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.att.tdp.issueflow.dto.request.CreateProjectRequest;
import com.att.tdp.issueflow.dto.request.UpdateProjectRequest;
import com.att.tdp.issueflow.exception.BusinessRuleException;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.model.entity.User;
import com.att.tdp.issueflow.model.enums.Role;
import com.att.tdp.issueflow.repository.ProjectRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import com.att.tdp.issueflow.support.TestPasswords;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ProjectServiceTest {

	@Autowired
	private ProjectService projectService;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private UserRepository userRepository;

	private Long ownerId;

	@BeforeEach
	void setUp() {
		projectRepository.deleteAll();
		userRepository.deleteAll();

		User owner = new User();
		owner.setUsername("owner");
		owner.setEmail("owner@example.com");
		owner.setFullName("Owner");
		owner.setRole(Role.ADMIN);
		owner.setPasswordHash(TestPasswords.encoded());
		ownerId = userRepository.save(owner).getId();
	}

	@Test
	void createProject_andGetById() {
		var created = projectService.createProject(createRequest("Sample Project"), ownerId);

		var fetched = projectService.getProjectById(created.getId());

		assertThat(fetched.getName()).isEqualTo("Sample Project");
		assertThat(fetched.getOwnerId()).isEqualTo(ownerId);
	}

	@Test
	void updateProject_changesName() {
		var created = projectService.createProject(createRequest("Sample Project"), ownerId);

		UpdateProjectRequest update = new UpdateProjectRequest();
		update.setName("Updated Name");

		var updated = projectService.updateProject(created.getId(), update, ownerId);

		assertThat(updated.getName()).isEqualTo("Updated Name");
	}

	@Test
	void softDeleteProject_hidesFromActiveList() {
		var created = projectService.createProject(createRequest("Sample Project"), ownerId);

		projectService.softDeleteProject(created.getId(), ownerId);

		assertThat(projectService.getAllProjects()).isEmpty();
		assertThat(projectService.getDeletedProjects(ownerId)).hasSize(1);
	}

	@Test
	void restoreProject_bringsBackActive() {
		var created = projectService.createProject(createRequest("Sample Project"), ownerId);
		projectService.softDeleteProject(created.getId(), ownerId);

		projectService.restoreProject(created.getId(), ownerId);

		assertThat(projectService.getAllProjects()).hasSize(1);
		assertThat(projectService.getProjectById(created.getId()).getName()).isEqualTo("Sample Project");
	}

	@Test
	void getDeletedProjects_rejectsNonAdmin() {
		Long devId = saveDeveloper("dev").getId();
		var created = projectService.createProject(createRequest("Sample Project"), ownerId);
		projectService.softDeleteProject(created.getId(), ownerId);

		assertThatThrownBy(() -> projectService.getDeletedProjects(devId))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("ADMIN");
	}

	@Test
	void restoreProject_rejectsNonAdmin() {
		Long devId = saveDeveloper("dev").getId();
		var created = projectService.createProject(createRequest("Sample Project"), ownerId);
		projectService.softDeleteProject(created.getId(), ownerId);

		assertThatThrownBy(() -> projectService.restoreProject(created.getId(), devId))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("ADMIN");
	}

	@Test
	void createProject_rejectsMissingOwner() {
		CreateProjectRequest request = createRequest("Orphan");
		request.setOwnerId(999L);

		assertThatThrownBy(() -> projectService.createProject(request, ownerId))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void updateProject_rejectsEmptyBody() {
		var created = projectService.createProject(createRequest("Sample Project"), ownerId);

		assertThatThrownBy(() -> projectService.updateProject(created.getId(), new UpdateProjectRequest(), ownerId))
				.isInstanceOf(BusinessRuleException.class);
	}

	private User saveDeveloper(String username) {
		User dev = new User();
		dev.setUsername(username);
		dev.setEmail(username + "@example.com");
		dev.setFullName("Developer");
		dev.setRole(Role.DEVELOPER);
		dev.setPasswordHash(TestPasswords.encoded());
		return userRepository.save(dev);
	}

	private CreateProjectRequest createRequest(String name) {
		CreateProjectRequest request = new CreateProjectRequest();
		request.setName(name);
		request.setDescription("A sample project");
		request.setOwnerId(ownerId);
		return request;
	}
}
