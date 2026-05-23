package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.model.entity.RevokedJwtToken;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RevokedJwtTokenRepository extends JpaRepository<RevokedJwtToken, Long> {

	boolean existsByJti(String jti);

	@Modifying
	@Query("DELETE FROM RevokedJwtToken r WHERE r.expiresAt < :cutoff")
	int deleteByExpiresAtBefore(@Param("cutoff") Instant cutoff);
}
