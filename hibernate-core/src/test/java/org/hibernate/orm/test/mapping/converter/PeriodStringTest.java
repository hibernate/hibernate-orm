/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converter;

import java.time.Period;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PeriodStringTest extends BaseEntityManagerFunctionalTestCase {

	private Period period = Period.ofYears(1).plusMonths(2).plusDays(3);

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Event.class
		};
	}

	@Test
	public void testLifecycle() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Event event = new Event(period);
			entityManager.persist(event);
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
			Event event = entityManager.createQuery("from Event", Event.class).getSingleResult();
			assertEquals(period, event.getSpan());
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::basic-jpa-convert-period-string-converter-immutability-plan-example[]
			Event event = entityManager.createQuery("from Event", Event.class).getSingleResult();
			event.setSpan(Period
				.ofYears(3)
				.plusMonths(2)
				.plusDays(1)
		);
			//end::basic-jpa-convert-period-string-converter-immutability-plan-example[]
		});
	}

	//tag::basic-jpa-convert-period-string-converter-mapping-example[]
	@Entity(name = "Event")
	public static class Event {

		@Id
		@GeneratedValue
		private Long id;

		@Convert(converter = PeriodStringConverter.class)
		@Column(columnDefinition = "")
		private Period span;

		//Getters and setters are omitted for brevity

	//end::basic-jpa-convert-period-string-converter-mapping-example[]

		public Event() {
		}

		public Event(Period span) {
			this.span = span;
		}

		public Long getId() {
			return id;
		}

		public Period getSpan() {
			return span;
		}

		public void setSpan(Period span) {
			this.span = span;
		}

		//tag::basic-jpa-convert-period-string-converter-mapping-example[]
	}
	//end::basic-jpa-convert-period-string-converter-mapping-example[]
}
