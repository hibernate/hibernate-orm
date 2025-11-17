/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
@Jpa( annotatedClasses = {LocalDateTimeWithTemporalTimeTest.DateEvent.class} )
public class LocalDateTimeWithTemporalTimeTest {

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			DateEvent dateEvent = new DateEvent(LocalDateTime.now());
			dateEvent.id = 1L;
			entityManager.persist(dateEvent);
		});
		scope.inTransaction( entityManager -> {
			DateEvent dateEvent = entityManager.find(DateEvent.class, 1L);
			assertNotNull(dateEvent.getTimestamp());
		});
	}

	@Entity(name = "DateEvent")
	public static class DateEvent {

		@Id
		private Long id;

		//throws org.hibernate.AnnotationException: @Temporal should only be set on a java.util.Date or java.util.Calendar property
		//@Temporal(TemporalType.TIME)
		@Column(name = "`timestamp`")
		private LocalDateTime timestamp;

		public DateEvent() {
		}

		public DateEvent(LocalDateTime timestamp) {
			this.timestamp = timestamp;
		}

		public Long getId() {
			return id;
		}

		public LocalDateTime getTimestamp() {
			return timestamp;
		}
	}
}
