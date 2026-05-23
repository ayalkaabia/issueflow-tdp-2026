package com.att.tdp.issueflow.controller.support;

import com.att.tdp.issueflow.model.enums.Role;
import com.att.tdp.issueflow.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public abstract class SecuredControllerTestBase {

	@BeforeEach
	void authenticateControllerTests() {
		AuthenticatedUser user = new AuthenticatedUser(1L, "tester", Role.ADMIN);
		UsernamePasswordAuthenticationToken authentication =
				new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}
}
