package com.att.tdp.issueflow.config;

import com.att.tdp.issueflow.security.JwtAuthenticationFilter;
import com.att.tdp.issueflow.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtService jwtService;
	private final ObjectMapper objectMapper;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService);
		http.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.POST, "/auth/login")
						.permitAll()
						.requestMatchers(HttpMethod.GET, "/tickets/deleted")
						.hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET, "/projects/deleted")
						.hasRole("ADMIN")
						.requestMatchers(HttpMethod.POST, "/tickets/*/restore")
						.hasRole("ADMIN")
						.requestMatchers(HttpMethod.POST, "/projects/*/restore")
						.hasRole("ADMIN")
						.anyRequest()
						.authenticated())
				.exceptionHandling(ex -> ex.authenticationEntryPoint(this::unauthorized)
						.accessDeniedHandler(this::forbidden))
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	private void unauthorized(
			HttpServletRequest request, HttpServletResponse response, Exception exception) throws java.io.IOException {
		writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
	}

	private void forbidden(
			HttpServletRequest request, HttpServletResponse response, Exception exception) throws java.io.IOException {
		writeError(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden");
	}

	private void writeError(HttpServletResponse response, int status, String message) throws java.io.IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), Map.of("message", message));
	}
}
