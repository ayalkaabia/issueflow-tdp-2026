package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.model.entity.CommentMention;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentMentionRepository extends JpaRepository<CommentMention, Long> {

	List<CommentMention> findByCommentId(Long commentId);

	List<CommentMention> findByCommentIdInOrderByIdAsc(Collection<Long> commentIds);

	void deleteByCommentId(Long commentId);

	long countByMentionedUserId(Long mentionedUserId);

	@Query(
			"""
			SELECT cm.commentId FROM CommentMention cm
			JOIN Comment c ON c.id = cm.commentId
			WHERE cm.mentionedUserId = :userId
			ORDER BY c.createdAt DESC
			""")
	Page<Long> findCommentIdsByMentionedUserId(@Param("userId") Long userId, Pageable pageable);
}
