package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.request.CreateUserRequest;
import com.att.tdp.issueflow.dto.request.UpdateUserRequest;
import com.att.tdp.issueflow.dto.response.UserResponse;
import com.att.tdp.issueflow.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Validated
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping
	public List<UserResponse> getAllUsers() {
		return userService.getAllUsers();
	}

	@GetMapping("/{userId}")
	public UserResponse getUserById(@PathVariable Long userId) {
		return userService.getUserById(userId);
	}

	@PostMapping
	public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
		return userService.createUser(request, null);
	}

	@PostMapping("/update/{userId}")
	public ResponseEntity<Void> updateUser(
			@PathVariable Long userId, @Valid @RequestBody UpdateUserRequest request) {
		userService.updateUser(userId, request, null);
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/{userId}")
	public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
		userService.deleteUser(userId, null);
		return ResponseEntity.ok().build();
	}
}
