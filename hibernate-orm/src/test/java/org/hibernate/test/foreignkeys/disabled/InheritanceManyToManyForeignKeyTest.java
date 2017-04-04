/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.foreignkeys.disabled;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToMany;
import javax.persistence.MappedSuperclass;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-9306")
public class InheritanceManyToManyForeignKeyTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				LocalDateEvent.class,
				UserEvents.class,
				ApplicationEvents.class
		};
	}

	@Test
	public void testForeignKeyNameUnicity() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		LocalDateEvent event1 = new LocalDateEvent();
		event1.startDate = LocalDate.of(1, 1, 1);
		session.persist(event1);

		LocalDateEvent event2 = new LocalDateEvent();
		event2.startDate = LocalDate.of(1, 1, 2);
		session.persist(event2);

		LocalDateEvent event3 = new LocalDateEvent();
		event3.startDate = LocalDate.of(1, 1, 3);
		session.persist(event3);

		UserEvents userEvents = new UserEvents();
		session.persist( userEvents );
		userEvents.getEvents().add( event1 );
		session.flush();
		userEvents.getEvents().add( event2 );
		session.flush();


		ApplicationEvents applicationEvents = new ApplicationEvents();
		session.persist( applicationEvents );
		applicationEvents.getEvents().add( event3 );

		transaction.commit();
		session.close();

		session = openSession();
		transaction = session.beginTransaction();

		assertEquals(2, session.get( UserEvents.class, userEvents.id ).getEvents().size());
		assertEquals(1, session.get( ApplicationEvents.class, applicationEvents.id ).getEvents().size());

		transaction.commit();
		session.close();
	}

	@Entity(name = "LDE")
	public static class LocalDateEvent {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "START_DATE", nullable = false)
		private LocalDate startDate;
	}

	@MappedSuperclass
	public static abstract class AbstractEventsEntityModel {

		@ManyToMany(fetch = FetchType.LAZY )
		private List<LocalDateEvent> events = new ArrayList<>(  );

		public List<LocalDateEvent> getEvents() {
			return events;
		}
	}

	@Entity(name = "UE")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class UserEvents extends AbstractEventsEntityModel {

		@Id
		@GeneratedValue
		private Long id;

	}

	@Entity(name = "AE")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class ApplicationEvents extends AbstractEventsEntityModel {

		@Id
		@GeneratedValue
		private Long id;

	}
}

