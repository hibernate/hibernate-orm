/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.community.dialect;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(InformixDialect.class)
@DomainModel( annotatedClasses = InformixFunctionTest.Event.class )
@SessionFactory
public class InformixFunctionTest {

	private Event event;

	@BeforeEach
	protected void prepareTest(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					event = new Event();
					event.country = "Romania";
					event.city = "Cluj-Napoca";
					session.persist( event );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10846" )
	public void testConcat(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					String location = (String) session.createQuery(
							"select concat(e.country, ' - ',  e.city) " +
									"from Event e " +
									"where e.id = :id")
							.setParameter( "id", event.id )
							.getSingleResult();
					assertEquals( "Romania - Cluj-Napoca", location);
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10846" )
	public void testSubstring(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					String location = (String) session.createQuery(
							"select substring(e.city, 0, 5) " +
									"from Event e " +
									"where e.id = :id")
							.setParameter( "id", event.id )
							.getSingleResult();
					assertEquals( "Cluj", location);
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10846" )
	public void testSubstr(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					String location = (String) session.createQuery(
							"select substr(e.city, 0, 4) " +
									"from Event e " +
									"where e.id = :id")
							.setParameter( "id", event.id )
							.getSingleResult();
					assertEquals( "Cluj", location);
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10846" )
	public void testCoalesceAndNvl(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					String location = (String) session.createQuery(
							"select coalesce(e.district, 'N/A') " +
									"from Event e " +
									"where e.id = :id")
							.setParameter( "id", event.id )
							.getSingleResult();
					assertEquals( "N/A", location);

					location = (String) session.createQuery(
							"select nvl(e.district, 'N/A') " +
									"from Event e " +
									"where e.id = :id")
							.setParameter( "id", event.id )
							.getSingleResult();
					assertEquals( "N/A", location);
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10800" )
	public void testCurrentDate(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Date date = (Date) session.createQuery(
							"select current_date() " +
									"from Event e " +
									"where e.id = :id")
							.setParameter( "id", event.id )
							.getSingleResult();

					assertNotNull( date );
					assertTrue( date.getTime() > 0 );

					Calendar resultCalendar = Calendar.getInstance();
					resultCalendar.setTime(date);

					assertEquals( 0, todayCalendar().compareTo(resultCalendar) );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10800" )
	public void testCurrentTimestamp(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					int tries = 2;
					while ( tries-- > 0 ) {
						Timestamp timestamp = (Timestamp) session.createQuery(
								"select current_timestamp() " +
										"from Event e " +
										"where e.id = :id" )
								.setParameter( "id", event.id )
								.getSingleResult();

						assertNotNull( timestamp );
						assertTrue( timestamp.getTime() > 0 );

						Calendar resultCalendar = Calendar.getInstance();
						resultCalendar.setTime( timestamp );

						long millis = resultCalendar.getTime().getTime() - todayCalendar().getTime().getTime();

						if(millis == 0) {
							//What are the odds that ou've run this test exactly at midnight?
							try {
								Thread.sleep( 1000 );
							}
							catch ( InterruptedException ignore ) {}
							continue;
						}

						assertTrue( millis > 0 );
					}
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-18369")
	public void testMatches(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					String country = (String) session.createQuery(
									"select e.country " +
											"from Event e " +
											"where e.id = :id and matches(e.country, :country) = 'T'" )
							.setParameter( "id", event.id )
							.setParameter( "country", "R*" )
							.getSingleResult();
					assertEquals( "Romania", country );
				}
		);
	}

	private Calendar todayCalendar() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar;
	}

	@SuppressWarnings("unused")
	@Entity(name = "Event")
	public static class Event {

		@Id
		@GeneratedValue
		private Long id;

		@Column
		private String country;

		private String city;

		private String district;
	}
}


