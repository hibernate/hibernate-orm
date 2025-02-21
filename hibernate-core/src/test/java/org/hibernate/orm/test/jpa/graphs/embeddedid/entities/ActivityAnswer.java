/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.graphs.embeddedid.entities;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "activity_answers")
public class ActivityAnswer {

	@EmbeddedId
	private ActivityAnswerId activityAnswerId;

	private String answer;

	public ActivityAnswer setActivityAnswerId(ActivityAnswerId activityAnswerId) {
		this.activityAnswerId = activityAnswerId;
		return this;
	}

	public ActivityAnswer setAnswer(String answer) {
		this.answer = answer;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ActivityAnswer that = (ActivityAnswer) o;
		return Objects.equals( activityAnswerId, that.activityAnswerId );
	}

	@Override
	public int hashCode() {
		return Objects.hashCode( activityAnswerId );
	}
}
