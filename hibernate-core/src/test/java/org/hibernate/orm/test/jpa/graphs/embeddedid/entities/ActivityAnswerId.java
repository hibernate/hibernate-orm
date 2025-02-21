/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.graphs.embeddedid.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;

import java.util.Objects;

@Embeddable
public class ActivityAnswerId {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumns({
			@JoinColumn(name = "exercise_id", referencedColumnName = "exercise_id"),
			@JoinColumn(name = "activity_id", referencedColumnName = "activity_id")
	})
	private Activity activity;

	@Column(name = "question_id")
	private String questionId;

	public ActivityAnswerId() {
	}

	public ActivityAnswerId setActivity(Activity activity) {
		this.activity = activity;
		return this;
	}

	public ActivityAnswerId setQuestionId(String questionId) {
		this.questionId = questionId;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ActivityAnswerId that = (ActivityAnswerId) o;
		return Objects.equals( activity, that.activity ) && Objects.equals( questionId, that.questionId );
	}

	@Override
	public int hashCode() {
		return Objects.hash( activity, questionId );
	}
}
