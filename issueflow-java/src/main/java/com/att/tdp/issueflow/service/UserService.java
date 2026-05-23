package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.request.CreateUserRequest;
import com.att.tdp.issueflow.dto.request.UpdateUserRequest;
import com.att.tdp.issueflow.dto.response.UserResponse;
import com.att.tdp.issueflow.exception.BusinessRuleException;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.mapper.UserMapper;
import com.att.tdp.issueflow.model.entity.User;
import com.att.tdp.issueflow.model.enums.AuditAction;
import com.att.tdp.issueflow.model.enums.AuditActor;
import com.att.tdp.issueflow.model.enums.AuditEntityType;
import com.att.tdp.issueflow.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuditService auditService;

	@Transactional(readOnly = true)
	public List<UserResponse> getAllUsers() {
		return userRepository.findAll().stream().map(UserMapper::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public UserResponse getUserById(Long userId) {
		return UserMapper.toResponse(requireUser(userId));
	}

	@Transactional
	public UserResponse createUser(CreateUserRequest request, Long performedBy) {
		if (userRepository.existsByUsername(request.getUsername())) {
			throw new BusinessRuleException("Username already exists: " + request.getUsername());
		}
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new BusinessRuleException("Email already exists: " + request.getEmail());
		}

		User user = new User();
		user.setUsername(request.getUsername());
		user.setEmail(request.getEmail());
		user.setFullName(request.getFullName());
		user.setRole(request.getRole());
		user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

		User saved = userRepository.save(user);
		auditService.log(AuditAction.CREATE, AuditEntityType.USER, saved.getId(), performedBy, AuditActor.USER);
		return UserMapper.toResponse(saved);
	}

	@Transactional
	public void updateUser(Long userId, UpdateUserRequest request, Long performedBy) {
		User user = requireUser(userId);

		if (!StringUtils.hasText(request.getFullName()) && request.getRole() == null) {
			throw new BusinessRuleException("At least one of fullName or role must be provided");
		}

		if (StringUtils.hasText(request.getFullName())) {
			user.setFullName(request.getFullName());
		}
		if (request.getRole() != null) {
			user.setRole(request.getRole());
		}

		userRepository.save(user);
		auditService.log(AuditAction.UPDATE, AuditEntityType.USER, userId, performedBy, AuditActor.USER);
	}

	@Transactional
	public void deleteUser(Long userId, Long performedBy) {
		User user = requireUser(userId);
		userRepository.delete(user);
		auditService.log(AuditAction.DELETE, AuditEntityType.USER, userId, performedBy, AuditActor.USER);
	}

	private User requireUser(Long userId) {
		return userRepository
				.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
	}
}
