/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.graphs.embeddedid.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "activities")
@NamedEntityGraph(name = "with.collections",
		attributeNodes = {
				@NamedAttributeNode(value = "answers"),
				@NamedAttributeNode(value = "documents")
		}
)
public class Activity {

	@EmbeddedId
	private ActivityExerciseId activityExerciseId;

	@MapsId("exerciseId")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "exercise_id")
	private Exercise exercise;

	@OneToMany(mappedBy = "activityAnswerId.activity", cascade = CascadeType.ALL)
	private Set<ActivityAnswer> answers = new HashSet<>();

	@OneToMany(mappedBy = "activityDocumentId.activity", orphanRemoval = true, cascade = CascadeType.ALL)
	private Set<ActivityDocument> documents = new HashSet<>();

	public Activity setActivityExerciseId(ActivityExerciseId activityExerciseId) {
		this.activityExerciseId = activityExerciseId;
		return this;
	}

	public Set<ActivityAnswer> getAnswers() {
		return answers;
	}

	public Set<ActivityDocument> getDocuments() {
		return documents;
	}

	public Activity setExercise(Exercise exercise) {
		this.exercise = exercise;
		return this;
	}

	public Activity setAnswers(Set<ActivityAnswer> answers) {
		this.answers = answers;
		return this;
	}

	public Activity setDocuments(Set<ActivityDocument> documents) {
		this.documents = documents;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Activity that = (Activity) o;
		return Objects.equals( activityExerciseId, that.activityExerciseId );
	}

	@Override
	public int hashCode() {
		return Objects.hashCode( activityExerciseId );
	}
}
