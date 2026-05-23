package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.config.JwtProperties;
import com.att.tdp.issueflow.exception.UnauthorizedException;
import com.att.tdp.issueflow.model.entity.RevokedJwtToken;
import com.att.tdp.issueflow.model.entity.User;
import com.att.tdp.issueflow.model.enums.Role;
import com.att.tdp.issueflow.repository.RevokedJwtTokenRepository;
import com.att.tdp.issueflow.security.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JwtService {

	private final JwtProperties jwtProperties;
	private final RevokedJwtTokenRepository revokedJwtTokenRepository;

	public IssuedToken generateToken(User user) {
		Instant now = Instant.now();
		Instant expiresAt = now.plusSeconds(jwtProperties.getExpirationSeconds());
		String jti = UUID.randomUUID().toString();

		String token = Jwts.builder()
				.id(jti)
				.subject(user.getUsername())
				.claim("uid", user.getId())
				.claim("role", user.getRole().name())
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiresAt))
				.signWith(signingKey())
				.compact();

		return new IssuedToken(token, jwtProperties.getExpirationSeconds(), jti, expiresAt);
	}

	public AuthenticatedUser authenticate(String token) {
		Claims claims = parseClaims(token);
		String jti = claims.getId();
		if (jti != null && revokedJwtTokenRepository.existsByJti(jti)) {
			throw new UnauthorizedException("Token has been revoked");
		}

		Long userId = extractUserId(claims);
		String username = claims.getSubject();
		String roleValue = claims.get("role", String.class);
		if (userId == null || username == null || roleValue == null) {
			throw new UnauthorizedException("Invalid token claims");
		}

		return new AuthenticatedUser(userId, username, Role.valueOf(roleValue));
	}

	@Transactional
	public void revokeToken(String token) {
		Claims claims = parseClaims(token);
		String jti = claims.getId();
		if (jti == null) {
			throw new UnauthorizedException("Invalid token");
		}
		if (revokedJwtTokenRepository.existsByJti(jti)) {
			return;
		}

		RevokedJwtToken revoked = new RevokedJwtToken();
		revoked.setJti(jti);
		revoked.setExpiresAt(claims.getExpiration().toInstant());
		revokedJwtTokenRepository.save(revoked);
	}

	private Claims parseClaims(String token) {
		try {
			return Jwts.parser()
					.verifyWith(signingKey())
					.build()
					.parseSignedClaims(token)
					.getPayload();
		} catch (JwtException | IllegalArgumentException ex) {
			throw new UnauthorizedException("Invalid or expired token");
		}
	}

	private Long extractUserId(Claims claims) {
		Object uid = claims.get("uid");
		if (uid instanceof Number number) {
			return number.longValue();
		}
		return null;
	}

	private SecretKey signingKey() {
		byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
		return Keys.hmacShaKeyFor(keyBytes);
	}

	public record IssuedToken(String accessToken, long expiresInSeconds, String jti, Instant expiresAt) {}
}
