/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.util.Date;
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
@Jpa( annotatedClasses = {DateWithTemporalTimestampTest.DateEvent.class} )
public class DateWithTemporalTimestampTest {

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			DateEvent dateEvent = new DateEvent(new Date());
			entityManager.persist(dateEvent);
		});
	}

	@Entity(name = "DateEvent")
	public static class DateEvent {

		@Id
		@GeneratedValue
		private Long id;

		//tag::basic-datetime-temporal-timestamp-example[]
		@Column(name = "`timestamp`")
		@Temporal(TemporalType.TIMESTAMP)
		private Date timestamp;
		//end::basic-datetime-temporal-timestamp-example[]

		public DateEvent() {
		}

		public DateEvent(Date timestamp) {
			this.timestamp = timestamp;
		}

		public Long getId() {
			return id;
		}

		public Date getTimestamp() {
			return timestamp;
		}
	}
}
