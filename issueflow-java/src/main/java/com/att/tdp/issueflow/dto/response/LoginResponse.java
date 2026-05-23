package com.att.tdp.issueflow.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

	private String accessToken;
	private String tokenType;
	private long expiresIn;
}
