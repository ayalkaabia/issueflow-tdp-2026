package com.att.tdp.issueflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.att.tdp.issueflow.dto.request.LoginRequest;
import com.att.tdp.issueflow.exception.UnauthorizedException;
import com.att.tdp.issueflow.model.entity.User;
import com.att.tdp.issueflow.model.enums.Role;
import com.att.tdp.issueflow.repository.RevokedJwtTokenRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import com.att.tdp.issueflow.security.AuthenticatedUser;
import com.att.tdp.issueflow.support.TestPasswords;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AuthServiceTest {

	@Autowired
	private AuthService authService;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RevokedJwtTokenRepository revokedJwtTokenRepository;

	@BeforeEach
	void setUp() {
		SecurityContextHolder.clearContext();
		revokedJwtTokenRepository.deleteAll();
		userRepository.deleteAll();

		User user = new User();
		user.setUsername("jdoe");
		user.setEmail("jdoe@example.com");
		user.setFullName("John Doe");
		user.setRole(Role.DEVELOPER);
		user.setPasswordHash(TestPasswords.encoded());
		userRepository.save(user);
	}

	@Test
	void login_returnsTokenForValidCredentials() {
		LoginRequest request = new LoginRequest();
		request.setUsername("jdoe");
		request.setPassword(TestPasswords.RAW);

		var response = authService.login(request);

		assertThat(response.getAccessToken()).isNotBlank();
		assertThat(response.getTokenType()).isEqualTo("Bearer");
		assertThat(response.getExpiresIn()).isEqualTo(3600);
	}

	@Test
	void login_rejectsInvalidPassword() {
		LoginRequest request = new LoginRequest();
		request.setUsername("jdoe");
		request.setPassword("wrong");

		assertThatThrownBy(() -> authService.login(request)).isInstanceOf(UnauthorizedException.class);
	}

	@Test
	void logout_revokesToken() {
		LoginRequest request = new LoginRequest();
		request.setUsername("jdoe");
		request.setPassword(TestPasswords.RAW);
		var login = authService.login(request);

		String token = login.getAccessToken();
		authService.logout("Bearer " + token);

		assertThatThrownBy(() -> jwtService.authenticate(token)).isInstanceOf(UnauthorizedException.class);
	}

	@Test
	void getCurrentUser_returnsAuthenticatedProfile() {
		User user = userRepository.findByUsername("jdoe").orElseThrow();
		AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getUsername(), user.getRole());
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

		var me = authService.getCurrentUser();

		assertThat(me.getUsername()).isEqualTo("jdoe");
		assertThat(me.getRole()).isEqualTo(Role.DEVELOPER);
	}
}
