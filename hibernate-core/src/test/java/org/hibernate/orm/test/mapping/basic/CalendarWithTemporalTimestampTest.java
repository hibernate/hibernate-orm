/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.util.Calendar;
import java.util.GregorianCalendar;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@Jpa( annotatedClasses = {CalendarWithTemporalTimestampTest.DateEvent.class} )
public class CalendarWithTemporalTimestampTest {

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		final Calendar calendar = new GregorianCalendar();
		scope.inTransaction( entityManager -> {
			entityManager.persist(new DateEvent(calendar));
		});
		scope.inTransaction( entityManager -> {
			DateEvent dateEvent = entityManager.createQuery("from DateEvent", DateEvent.class).getSingleResult();
			//Assert.assertEquals(calendar, dateEvent.getTimestamp());
		});
	}

	@Entity(name = "DateEvent")
	public static class DateEvent {

		@Id
		@GeneratedValue
		private Long id;

		@Temporal(TemporalType.TIMESTAMP)
		@Column(name = "`timestamp`")
		private Calendar timestamp;

		public DateEvent() {
		}

		public DateEvent(Calendar timestamp) {
			this.timestamp = timestamp;
		}

		public Long getId() {
			return id;
		}

		public Calendar getTimestamp() {
			return timestamp;
		}
	}
}
