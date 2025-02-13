/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.graphs.embeddedid.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class ActivityExerciseId {

	private Integer exerciseId;

	@Column(name = "activity_id")
	private String activityId;

	public ActivityExerciseId() {
	}

	public ActivityExerciseId setExerciseId(Integer exerciseId) {
		this.exerciseId = exerciseId;
		return this;
	}

	public ActivityExerciseId setActivityId(String activityId) {
		this.activityId = activityId;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ActivityExerciseId that = (ActivityExerciseId) o;
		return Objects.equals( exerciseId, that.exerciseId ) && Objects.equals( activityId,
				that.activityId );
	}

	@Override
	public int hashCode() {
		return Objects.hash( exerciseId, activityId );
	}
}
