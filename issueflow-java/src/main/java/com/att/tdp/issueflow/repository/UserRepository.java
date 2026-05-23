package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.model.entity.User;
import com.att.tdp.issueflow.model.enums.Role;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsername(String username);

	Optional<User> findByUsernameIgnoreCase(String username);

	List<User> findByUsernameIn(Collection<String> usernames);

	List<User> findByUsernameIgnoreCaseIn(Collection<String> usernames);

	Optional<User> findByEmail(String email);

	boolean existsByUsername(String username);

	boolean existsByEmail(String email);

	List<User> findByRole(Role role);
}
