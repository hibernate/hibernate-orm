/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.timestamp;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalTime;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.MySQL5Dialect;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialectFeature(DialectChecks.SupportsJdbcDriverProxying.class)
@TestForIssue(jiraKey = "HHH-12988")
@SkipForDialect(MySQL5Dialect.class)
public class JdbcTimestampDSTWithUTCTest
		extends BaseNonConfigCoreFunctionalTestCase {

	protected final Logger log = Logger.getLogger( getClass() );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class
		};
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put(
				AvailableSettings.JDBC_TIME_ZONE,
				"UTC"
		);
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Test
	public void testHibernate() {
		doInHibernate( this::sessionFactory, session -> {
			Person person = new Person();

			person.setId( 1L );
			person.setShiftStartTime( LocalTime.of( 12, 0, 0 ) );

			session.persist( person );
		} );

		doInHibernate( this::sessionFactory, session -> {
			Person person = session.find( Person.class, 1L );

			assertEquals( LocalTime.of( 12, 0, 0 ), person.getShiftStartTime() );
		} );
	}

	@Test
	public void testJDBC() {
		doInHibernate( this::sessionFactory, session -> {
			session.doWork( connection -> {

				Time time = Time.valueOf( LocalTime.of( 12, 0, 0 ) );

				try (PreparedStatement ps = connection.prepareStatement(
						"INSERT INTO Person (id, shiftStartTime) VALUES (?, ?)" )) {
					ps.setLong( 1, 1L );
					ps.setTime( 2, time, new GregorianCalendar( TimeZone.getTimeZone( "UTC" ) ) );

					ps.executeUpdate();
				}

				try (Statement st = connection.createStatement()) {
					try (ResultSet rs = st.executeQuery( "SELECT shiftStartTime FROM Person WHERE id = 1" )) {
						while ( rs.next() ) {
							Time dbTime = rs.getTime( 1, new GregorianCalendar( TimeZone.getTimeZone( "UTC" ) ) );

							assertEquals( time, dbTime );
						}
					}
				}
			} );
		} );
	}

	@Test
	public void testDBTimeValueAsEpochDST() {
		doInHibernate( this::sessionFactory, session -> {
			session.doWork( connection -> {

				Time time = Time.valueOf( LocalTime.of( 12, 0, 0 ) );

				GregorianCalendar utcCalendar = new GregorianCalendar( TimeZone.getTimeZone( "UTC" ) );
				utcCalendar.setTimeInMillis( time.getTime() );
				LocalTime utcLocalTime = utcCalendar.toZonedDateTime().toLocalTime();
				Time utcTime = Time.valueOf( utcLocalTime );

				try (PreparedStatement ps = connection.prepareStatement(
						"INSERT INTO Person (id, shiftStartTime) VALUES (?, ?)" )) {
					ps.setLong( 1, 1L );
					ps.setTime( 2, time, new GregorianCalendar( TimeZone.getTimeZone( "UTC" ) ) );

					ps.executeUpdate();
				}

				try (Statement st = connection.createStatement()) {
					try (ResultSet rs = st.executeQuery( "SELECT shiftStartTime FROM Person WHERE id = 1" )) {
						while ( rs.next() ) {
							Time dbTime = rs.getTime( 1 );

							assertEquals( utcTime, dbTime );
						}
					}
				}
			} );
		} );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private LocalTime shiftStartTime;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public LocalTime getShiftStartTime() {
			return shiftStartTime;
		}

		public void setShiftStartTime(LocalTime shiftStartTime) {
			this.shiftStartTime = shiftStartTime;
		}
	}
}

