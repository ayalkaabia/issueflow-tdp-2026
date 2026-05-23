package com.att.tdp.issueflow.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.att.tdp.issueflow.dto.response.AuditLogResponse;
import com.att.tdp.issueflow.model.enums.AuditAction;
import com.att.tdp.issueflow.model.enums.AuditActor;
import com.att.tdp.issueflow.model.enums.AuditEntityType;
import com.att.tdp.issueflow.service.AuditLogService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(value = AuditLogController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class AuditLogControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuditLogService auditLogService;

	@Test
	void getAuditLogs_withFilters() throws Exception {
		AuditLogResponse log = AuditLogResponse.builder()
				.id(1L)
				.action(AuditAction.CREATE)
				.entityType(AuditEntityType.TICKET)
				.entityId(5L)
				.performedBy(2L)
				.actor(AuditActor.USER)
				.timestamp(Instant.parse("2026-03-01T10:00:00Z"))
				.build();

		when(auditLogService.getAuditLogs(
						AuditEntityType.TICKET, 5L, AuditAction.CREATE, AuditActor.USER))
				.thenReturn(List.of(log));

		mockMvc.perform(get("/audit-logs")
						.param("entityType", "TICKET")
						.param("entityId", "5")
						.param("action", "CREATE")
						.param("actor", "USER"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].entityType").value("TICKET"))
				.andExpect(jsonPath("$[0].performedBy").value(2));

		verify(auditLogService)
				.getAuditLogs(AuditEntityType.TICKET, 5L, AuditAction.CREATE, AuditActor.USER);
	}
}
