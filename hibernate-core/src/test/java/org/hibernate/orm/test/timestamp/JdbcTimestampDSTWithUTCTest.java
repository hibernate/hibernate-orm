/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.timestamp;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalTime;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.MySQLDialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.jboss.logging.Logger;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Vlad Mihalcea
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJdbcDriverProxying.class)
@JiraKey(value = "HHH-12988")
@SkipForDialect(dialectClass = MySQLDialect.class, matchSubTypes = true)
@DomainModel(
		annotatedClasses = JdbcTimestampDSTWithUTCTest.Person.class
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.JDBC_TIME_ZONE, value = "UTC")
)
public class JdbcTimestampDSTWithUTCTest {

	protected final Logger log = Logger.getLogger( getClass() );

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testHibernate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Person person = new Person();

			person.setId( 1L );
			person.setShiftStartTime( LocalTime.of( 12, 0, 0 ) );

			session.persist( person );
		} );

		scope.inTransaction( session -> {
			Person person = session.find( Person.class, 1L );

			assertEquals( LocalTime.of( 12, 0, 0 ), person.getShiftStartTime() );
		} );
	}

	@Test
	public void testJDBC(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
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
	public void testDBTimeValueAsEpochDST(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
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
