package com.att.tdp.issueflow.model.enums;

public enum TicketStatus {
	TODO,
	IN_PROGRESS,
	IN_REVIEW,
	DONE;

	public boolean canTransitionTo(TicketStatus target) {
		if (target == null || this == DONE) {
			return false;
		}
		if (target == this) {
			return true;
		}
		return switch (this) {
			case TODO -> target == IN_PROGRESS;
			case IN_PROGRESS -> target == IN_REVIEW;
			case IN_REVIEW -> target == DONE;
			case DONE -> false;
		};
	}
}
