package com.att.tdp.issueflow.security;

import com.att.tdp.issueflow.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

	private SecurityUtils() {}

	public static AuthenticatedUser getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
			throw new UnauthorizedException("Not authenticated");
		}
		return user;
	}

	public static Long getCurrentUserId() {
		return getCurrentUser().getId();
	}
}
