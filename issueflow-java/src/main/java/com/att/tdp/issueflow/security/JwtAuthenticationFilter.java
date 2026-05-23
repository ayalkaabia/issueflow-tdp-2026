package com.att.tdp.issueflow.security;

import com.att.tdp.issueflow.exception.UnauthorizedException;
import com.att.tdp.issueflow.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String token = resolveToken(request.getHeader(HttpHeaders.AUTHORIZATION));

		if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
			try {
				AuthenticatedUser user = jwtService.authenticate(token);
				UsernamePasswordAuthenticationToken authentication =
						new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			} catch (UnauthorizedException ignored) {
				// Invalid token: leave context unauthenticated; secured endpoints return 401.
			}
		}
		filterChain.doFilter(request, response);
	}

	private String resolveToken(String authorizationHeader) {
		if (!StringUtils.hasText(authorizationHeader)) {
			return null;
		}
		String value = authorizationHeader.trim();
		if (value.length() < 6 || !value.regionMatches(true, 0, "Bearer", 0, 6)) {
			return null;
		}
		value = value.substring(6).trim();
		if (value.regionMatches(true, 0, "Bearer", 0, 6)) {
			value = value.substring(6).trim();
		}
		if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
			value = value.substring(1, value.length() - 1).trim();
		}
		return StringUtils.hasText(value) ? value : null;
	}
}
