package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.model.entity.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

	List<Project> findByDeletedAtIsNull();

	List<Project> findByDeletedAtIsNotNull();

	Optional<Project> findByIdAndDeletedAtIsNull(Long id);

	Optional<Project> findByIdAndDeletedAtIsNotNull(Long id);
}
