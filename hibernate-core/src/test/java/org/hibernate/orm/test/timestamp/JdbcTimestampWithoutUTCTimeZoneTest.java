/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.timestamp;

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import org.hibernate.testing.orm.jdbc.TimeZoneConnectionProvider;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = PostgreSQLDialect.class)
public class JdbcTimestampWithoutUTCTimeZoneTest extends BaseSessionFactoryFunctionalTest {

	private TimeZoneConnectionProvider connectionProvider;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class
		};
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builder) {
		connectionProvider = new TimeZoneConnectionProvider( "America/Los_Angeles" );
		connectionProvider.setConnectionProvider( (ConnectionProvider) builder.getSettings()
				.get( AvailableSettings.CONNECTION_PROVIDER ) );
		builder.applySetting(
				AvailableSettings.CONNECTION_PROVIDER,
				connectionProvider
		);
	}

	@AfterAll
	protected void releaseResources() {
		if ( connectionProvider != null ) {
			connectionProvider.stop();
		}
	}

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "https://github.com/cockroachdb/cockroach/issues/3781")
	public void testTimeZone() {
		inTransaction( session -> {
			Person person = new Person();
			person.id = 1L;
			long y2kMillis = LocalDateTime.of( 2000, 1, 1, 0, 0, 0 )
					.atZone( ZoneId.of( "UTC" ) )
					.toInstant()
					.toEpochMilli();
			assertEquals( 946684800000L, y2kMillis );

			person.createdOn = new Timestamp( y2kMillis );
			session.persist( person );

		} );
		inTransaction( s -> {
			s.doWork( connection -> {
				try (Statement st = connection.createStatement()) {
					try (ResultSet rs = st.executeQuery(
							"SELECT to_char(createdon, 'YYYY-MM-DD HH24:MI:SS.US') " +
									"FROM person" )) {
						while ( rs.next() ) {
							String timestamp = rs.getString( 1 );
							assertEquals( expectedTimestampValue(), timestamp );
						}
					}
				}
			} );
		} );
	}

	protected String expectedTimestampValue() {
		return "1999-12-31 16:00:00.000000";
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private Timestamp createdOn;
	}
}
