/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.graphs.embeddedid.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "exercises")
public class Exercise {

	@Id
	@Column(name = "exercise_id")
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "exercises_sequence")
	@SequenceGenerator(
			name = "exercises_sequence",
			sequenceName = "exercises_sequence",
			allocationSize = 1
	)

	private Integer id;

	public Integer getId() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Exercise exercise = (Exercise) o;
		return Objects.equals( id, exercise.id );
	}

	@Override
	public int hashCode() {
		return Objects.hashCode( id );
	}
}
