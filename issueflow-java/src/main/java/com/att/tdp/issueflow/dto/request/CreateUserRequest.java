package com.att.tdp.issueflow.dto.request;

import com.att.tdp.issueflow.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {

	@NotBlank
	private String username;

	@NotBlank
	@Email
	private String email;

	@NotBlank
	private String fullName;

	@NotNull
	private Role role;

	@NotBlank
	private String password;
}
