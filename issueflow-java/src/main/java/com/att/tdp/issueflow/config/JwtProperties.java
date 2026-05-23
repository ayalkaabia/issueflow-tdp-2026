package com.att.tdp.issueflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "issueflow.jwt")
@Getter
@Setter
public class JwtProperties {

	private String secret = "issueflow-dev-secret-change-in-production-min-32-chars";
	private long expirationSeconds = 3600;
}
