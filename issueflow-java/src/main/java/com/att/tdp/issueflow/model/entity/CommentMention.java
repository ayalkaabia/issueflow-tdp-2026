package com.att.tdp.issueflow.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
		name = "comment_mentions",
		uniqueConstraints = @UniqueConstraint(columnNames = {"comment_id", "mentioned_user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class CommentMention {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "comment_id", nullable = false)
	private Long commentId;

	@Column(name = "mentioned_user_id", nullable = false)
	private Long mentionedUserId;
}
