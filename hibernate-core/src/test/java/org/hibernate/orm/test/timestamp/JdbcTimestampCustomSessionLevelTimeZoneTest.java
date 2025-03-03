/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.timestamp;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@SkipForDialect(dialectClass = MySQLDialect.class, matchSubTypes = true)
public class JdbcTimestampCustomSessionLevelTimeZoneTest
		extends BaseSessionFactoryFunctionalTest {

	private PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider();

	private static final TimeZone TIME_ZONE = TimeZone.getTimeZone(
			"America/Los_Angeles" );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class
		};
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builder) {
		connectionProvider.setConnectionProvider( (ConnectionProvider) builder.getSettings().get( AvailableSettings.CONNECTION_PROVIDER ) );
		builder.applySetting(
				AvailableSettings.CONNECTION_PROVIDER,
				connectionProvider
		);
	}

	@AfterAll
	protected void releaseResources() {
		connectionProvider.stop();
	}

	@Test
	public void testTimeZone() throws Throwable {

		connectionProvider.clear();
		doInHibernateSessionBuilder( () -> {
			return sessionFactory().withOptions().jdbcTimeZone( TIME_ZONE );
		}, s -> {
			Person person = new Person();
			person.id = 1L;
			s.persist( person );

		} );

		assertEquals( 1, connectionProvider.getPreparedStatements().size() );
		PreparedStatement ps = connectionProvider.getPreparedStatements()
				.get( 0 );
		List<Object[]> setTimeCalls = connectionProvider.spyContext.getCalls(
				PreparedStatement.class.getMethod( "setTimestamp", int.class, Timestamp.class, Calendar.class ),
				ps
		);
		assertEquals( 1, setTimeCalls.size() );
		assertEquals(
				TIME_ZONE,
				( (Calendar) setTimeCalls.get( 0 )[2] ).getTimeZone()
		);

		connectionProvider.clear();
		doInHibernateSessionBuilder( () -> {
			return sessionFactory().withOptions().jdbcTimeZone( TIME_ZONE );
		}, s -> {
			s.doWork( connection -> {
				try ( Statement st = connection.createStatement() ) {
					try ( ResultSet rs = st.executeQuery(
							"select createdOn from Person" ) ) {
						while ( rs.next() ) {
							Timestamp timestamp = rs.getTimestamp( 1 );
							int offsetDiff = TimeZone.getDefault()
									.getOffset( 0 ) - TIME_ZONE.getOffset( 0 );
							assertEquals(
									Math.abs( Long.valueOf( offsetDiff )
													.longValue() ),
									Math.abs( timestamp.getTime() )
							);
						}
					}
				}
			} );
			Person person = s.find( Person.class, 1L );
			assertEquals( 0, person.createdOn.getTime() );
		} );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private Timestamp createdOn = new Timestamp( 0 );
	}
}
