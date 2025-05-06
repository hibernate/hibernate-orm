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

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class DateWithTemporalDateTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				DateEvent.class
		};
	}

	@Test
	public void testLifecycle() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::basic-datetime-temporal-date-persist-example[]
			DateEvent dateEvent = new DateEvent(new Date());
			entityManager.persist(dateEvent);
			//end::basic-datetime-temporal-date-persist-example[]
		});
	}

	//tag::basic-datetime-temporal-date-example[]
	@Entity(name = "DateEvent")
	public static class DateEvent {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "`timestamp`")
		@Temporal(TemporalType.DATE)
		private Date timestamp;

		//Getters and setters are omitted for brevity

	//end::basic-datetime-temporal-date-example[]
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
	//tag::basic-datetime-temporal-date-example[]
	}
	//end::basic-datetime-temporal-date-example[]
}
