/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-10465")
@SkipForDialect(MariaDBDialect.class)
@SkipForDialect(MySQLDialect.class)
@SkipForDialect(value = OracleDialect.class, comment = "Oracle date does not support milliseconds  ")
@SkipForDialect(value = HANADialect.class, comment = "HANA date does not support milliseconds  ")
public class TimeAndTimestampTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
			Event.class
		};
	}

	@Test
	public void test() {
		doInHibernate( this::sessionFactory, session -> {
			Event event = new Event();
			event.id = 1L;
			event.timeValue = new Time( 1000 );
			event.timestampValue = new Timestamp( 45677 );

			session.persist( event );
		} );
		doInHibernate( this::sessionFactory, session -> {
			Event event = session.find( Event.class, 1L );
			assertEquals(1000, event.timeValue.getTime() % TimeUnit.DAYS.toMillis( 1 ));
			assertEquals(45677, event.timestampValue.getTime() % TimeUnit.DAYS.toMillis( 1 ));
		} );
	}

	@Entity(name = "Event")
	public static class Event {

		@Id
		private Long id;

		private Time timeValue;

		private Timestamp timestampValue;
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}
}
