package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.request.LoginRequest;
import com.att.tdp.issueflow.dto.response.LoginResponse;
import com.att.tdp.issueflow.dto.response.UserResponse;
import com.att.tdp.issueflow.exception.UnauthorizedException;
import com.att.tdp.issueflow.mapper.UserMapper;
import com.att.tdp.issueflow.model.entity.User;
import com.att.tdp.issueflow.repository.UserRepository;
import com.att.tdp.issueflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request) {
		User user = userRepository
				.findByUsername(request.getUsername())
				.orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

		if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
			throw new UnauthorizedException("Invalid username or password");
		}

		JwtService.IssuedToken issuedToken = jwtService.generateToken(user);
		return LoginResponse.builder()
				.accessToken(issuedToken.accessToken())
				.tokenType("Bearer")
				.expiresIn(issuedToken.expiresInSeconds())
				.build();
	}

	@Transactional
	public void logout(String authorizationHeader) {
		String token = extractBearerToken(authorizationHeader);
		jwtService.revokeToken(token);
	}

	@Transactional(readOnly = true)
	public UserResponse getCurrentUser() {
		Long userId = SecurityUtils.getCurrentUserId();
		User user = userRepository
				.findById(userId)
				.orElseThrow(() -> new UnauthorizedException("User not found"));
		return UserMapper.toResponse(user);
	}

	private String extractBearerToken(String authorizationHeader) {
		if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
			throw new UnauthorizedException("Authorization header with Bearer token is required");
		}
		return authorizationHeader.substring(7).trim();
	}
}
