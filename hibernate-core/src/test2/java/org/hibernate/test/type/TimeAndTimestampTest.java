/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-10465")
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
			event.timeValue = new Time( 123 );
			event.timestampValue = new Timestamp( 456 );

			session.persist( event );
		} );
		doInHibernate( this::sessionFactory, session -> {
			Event event = session.find( Event.class, 1L );
			assertEquals(123, event.timeValue.getTime() % TimeUnit.DAYS.toMillis( 1 ));
			assertEquals(456, event.timestampValue.getTime() % TimeUnit.DAYS.toMillis( 1 ));
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
