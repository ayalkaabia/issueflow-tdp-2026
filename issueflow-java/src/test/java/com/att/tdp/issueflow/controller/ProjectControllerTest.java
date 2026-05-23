package com.att.tdp.issueflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.att.tdp.issueflow.controller.support.SecuredControllerTestBase;
import com.att.tdp.issueflow.dto.response.ProjectResponse;
import com.att.tdp.issueflow.dto.response.WorkloadResponse;
import com.att.tdp.issueflow.service.ProjectService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = ProjectController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectControllerTest extends SecuredControllerTestBase {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProjectService projectService;

	@Test
	void getAllProjects_returnsList() throws Exception {
		when(projectService.getAllProjects()).thenReturn(List.of(sampleProject()));

		mockMvc.perform(get("/projects"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Sample Project"));
	}

	@Test
	void createProject_returnsCreatedProject() throws Exception {
		when(projectService.createProject(any(), eq(1L))).thenReturn(sampleProject());

		mockMvc.perform(
						post("/projects")
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "name": "Sample Project",
										  "description": "A sample project",
										  "ownerId": 1
										}
										"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ownerId").value(1));

		verify(projectService).createProject(any(), eq(1L));
	}

	@Test
	void updateProject_acceptsPatch() throws Exception {
		when(projectService.updateProject(eq(1L), any(), eq(1L))).thenReturn(sampleProject());

		mockMvc.perform(
						patch("/projects/1")
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "name": "Updated Name"
										}
										"""))
				.andExpect(status().isOk());

		verify(projectService).updateProject(eq(1L), any(), eq(1L));
	}

	@Test
	void deleteProject_returnsOk() throws Exception {
		doNothing().when(projectService).softDeleteProject(eq(1L), eq(1L));

		mockMvc.perform(delete("/projects/1")).andExpect(status().isOk());

		verify(projectService).softDeleteProject(eq(1L), eq(1L));
	}

	@Test
	void getProjectWorkload_usesCorrectPath() throws Exception {
		WorkloadResponse workload = WorkloadResponse.builder()
				.userId(1L)
				.username("jdoe")
				.openTicketCount(3L)
				.build();
		when(projectService.getProjectWorkload(1L)).thenReturn(List.of(workload));

		mockMvc.perform(get("/projects/1/workload"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].username").value("jdoe"))
				.andExpect(jsonPath("$[0].openTicketCount").value(3));

		verify(projectService).getProjectWorkload(1L);
	}

	@Test
	void getDeletedProjects_usesCorrectPath() throws Exception {
		when(projectService.getDeletedProjects(1L)).thenReturn(List.of(sampleProject()));

		mockMvc.perform(get("/projects/deleted"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1));

		verify(projectService).getDeletedProjects(1L);
	}

	@Test
	void restoreProject_usesCorrectPath() throws Exception {
		doNothing().when(projectService).restoreProject(eq(1L), eq(1L));

		mockMvc.perform(post("/projects/1/restore")).andExpect(status().isOk());

		verify(projectService).restoreProject(eq(1L), eq(1L));
	}

	private ProjectResponse sampleProject() {
		return ProjectResponse.builder()
				.id(1L)
				.name("Sample Project")
				.description("A sample project")
				.ownerId(1L)
				.build();
	}
}
