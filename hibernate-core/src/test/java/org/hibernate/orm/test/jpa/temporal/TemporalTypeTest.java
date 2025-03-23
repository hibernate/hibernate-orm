/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.temporal;

import java.util.Calendar;
import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Query;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

/**
 * @author Brett Meyer
 */
@Jpa(annotatedClasses = {
		TemporalTypeTest.DataPoint.class
})
public class TemporalTypeTest {

	@Test
	public void testTemporalType(EntityManagerFactoryScope scope) {
		Date date = new Date();
		Calendar calendar = Calendar.getInstance();
		scope.inTransaction(
				entityManager -> {
					DataPoint dp = new DataPoint();
					dp.date1 = date;
					dp.date2 = date;
					dp.calendar1 = calendar;
					dp.calendar2 = calendar;
					entityManager.persist( dp );
				}
		);

		doTest(scope, "date1", date);
		doTest(scope, "date1", calendar);
		doTest(scope, "date2", date);
		doTest(scope, "date2", calendar);

		doTest(scope, "calendar1", date);
		doTest(scope, "calendar1", calendar);
		doTest(scope, "calendar2", date);
		doTest(scope, "calendar2", calendar);
	}

	private void doTest(EntityManagerFactoryScope scope, String property, Object obj) {
		doTest( scope, property, obj, TemporalType.DATE );
		doTest( scope, property, obj, TemporalType.TIMESTAMP );
	}

	private void doTest(EntityManagerFactoryScope scope, String property, Object obj, TemporalType temporalType) {
		scope.inTransaction(
				entityManager -> {
					Query query = entityManager.createQuery("from DataPoint where " + property + " = :obj");
					if (obj instanceof Calendar) {
						query.setParameter("obj", (Calendar) obj, temporalType);
					}
					else {
						query.setParameter("obj", (Date) obj, temporalType);
					}
				}
		);
	}

	@Entity(name = "DataPoint")
	public static class DataPoint {
		@Id @GeneratedValue
		public long id;

		@Temporal( TemporalType.DATE )
		public Date date1;

		@Temporal( TemporalType.TIMESTAMP )
		public Date date2;

		@Temporal( TemporalType.DATE )
		public Calendar calendar1;

		@Temporal( TemporalType.TIMESTAMP )
		public Calendar calendar2;
	}
}
