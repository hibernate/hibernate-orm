package org.hibernate.engine.query;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.InformixDialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(InformixDialect.class)
public class InformixFunctionTest extends BaseCoreFunctionalTestCase {

	private Event event;

	@Override
	protected void prepareTest() throws Exception {
		Session s = openSession();
		try {
			Transaction transaction = s.beginTransaction();
			event = new Event();
			event.country = "Romania";
			event.city = "Cluj-Napoca";
			s.persist( event );
			transaction.commit();
		}
		finally {
			s.close();
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10846" )
	public void testConcat() throws Exception {
		Session s = openSession();
		try {
			s.beginTransaction();
			String location = (String) session.createQuery(
				"select concat(e.country, ' - ',  e.city) " +
				"from Event e " +
				"where e.id = :id")
			.setParameter( "id", event.id )
			.getSingleResult();
			assertEquals( "Romania - Cluj-Napoca", location);

			s.getTransaction().commit();
		}
		finally {
			s.close();
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10846" )
	public void testSubstring() throws Exception {
		Session s = openSession();
		try {
			s.beginTransaction();
			String location = (String) session.createQuery(
				"select substring(e.city, 0, 5) " +
				"from Event e " +
				"where e.id = :id")
			.setParameter( "id", event.id )
			.getSingleResult();
			assertEquals( "Cluj", location);

			s.getTransaction().commit();
		}
		finally {
			s.close();
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10846" )
	public void testSubstr() throws Exception {
		Session s = openSession();
		try {
			s.beginTransaction();
			String location = (String) session.createQuery(
				"select substr(e.city, 0, 4) " +
				"from Event e " +
				"where e.id = :id")
			.setParameter( "id", event.id )
			.getSingleResult();
			assertEquals( "Cluj", location);

			s.getTransaction().commit();
		}
		finally {
			s.close();
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10846" )
	public void testCoalesceAndNvl() throws Exception {
		Session s = openSession();
		try {
			s.beginTransaction();
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

			s.getTransaction().commit();
		}
		finally {
			s.close();
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Event.class
		};
	}

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


