package com.att.tdp.issueflow.dto.request;

import com.att.tdp.issueflow.model.enums.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {

	private String fullName;

	private Role role;
}
