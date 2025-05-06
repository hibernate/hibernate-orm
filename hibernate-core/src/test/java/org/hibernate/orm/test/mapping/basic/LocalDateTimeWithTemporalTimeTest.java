/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class LocalDateTimeWithTemporalTimeTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				DateEvent.class
		};
	}

	@Test
	public void testLifecycle() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			DateEvent dateEvent = new DateEvent(LocalDateTime.now());
			dateEvent.id = 1L;
			entityManager.persist(dateEvent);
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
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
