package com.att.tdp.issueflow.dto.response;

import com.att.tdp.issueflow.model.enums.Role;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

	private Long id;
	private String username;
	private String email;
	private String fullName;
	private Role role;
}
