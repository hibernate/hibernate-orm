/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.customtype;


import org.hibernate.envers.Audited;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.junit.jupiter.api.Test;

import java.time.Duration;

@JiraKey(value = "HHH-17243")
@EnversTest
@Jpa(annotatedClasses = DurationTest.DurationTestEntity.class)
public class DurationTest {

	@Entity(name = "Duration")
	@Audited
	public static class DurationTestEntity {
		@Id
		@GeneratedValue
		private Integer id;

		private Duration duration;

		DurationTestEntity() {

		}

		DurationTestEntity(Duration aDuration) {
			this.duration = aDuration;
		}

		public Integer getId() {
			return id;
		}

		public Duration getDuration() {
			return duration;
		}

		public void setDuration(Duration aDuration) {
			this.duration = aDuration;
		}
	}

	private Integer durationId;

	@Test
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1 - insert
		this.durationId = scope.fromTransaction( entityManager -> {
			final DurationTestEntity duration = new DurationTestEntity( Duration.ofHours( 2 ) );
			entityManager.persist( duration );
			return duration.getId();
		} );

		// Revision 2 - update
		scope.inTransaction( entityManager -> {
			final DurationTestEntity duration = entityManager.find( DurationTestEntity.class, this.durationId );
			duration.setDuration( Duration.ofHours( 3 ) );
			entityManager.merge( duration );
		} );
	}
}
