/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.timestamp;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.Instant;
import java.time.OffsetTime;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Vlad Mihalcea
 */
@SkipForDialect(dialectClass = MySQLDialect.class, matchSubTypes = true)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJdbcDriverProxying.class)
public class JdbcTimeCustomTimeZoneTest
		extends BaseSessionFactoryFunctionalTest {

	private PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider(
			true,
			false
	);

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
		connectionProvider.setConnectionProvider( (ConnectionProvider) builder.getSettings()
				.get( AvailableSettings.CONNECTION_PROVIDER ) );
		builder.applySetting(
				AvailableSettings.CONNECTION_PROVIDER,
				connectionProvider
		);
		builder.applySetting(
				AvailableSettings.JDBC_TIME_ZONE,
				TIME_ZONE
		);
	}

	@AfterAll
	protected void releaseResources() {
		connectionProvider.stop();
	}

	@Test
	public void testTimeZone() {

		connectionProvider.clear();
		inTransaction( s -> {
			Person person = new Person();
			person.id = 1L;
			s.persist( person );

		} );

		assertEquals( 1, connectionProvider.getPreparedStatements().size() );
		PreparedStatement ps = connectionProvider.getPreparedStatements()
				.get( 0 );
		try {
			ArgumentCaptor<Calendar> calendarArgumentCaptor = ArgumentCaptor.forClass(
					Calendar.class );
			verify( ps, times( 1 ) ).setTime(
					anyInt(),
					any( Time.class ),
					calendarArgumentCaptor.capture()
			);
			assertEquals(
					TIME_ZONE,
					calendarArgumentCaptor.getValue().getTimeZone()
			);
		}
		catch (SQLException e) {
			fail( e.getMessage() );
		}

		connectionProvider.clear();
		inTransaction( s -> {
			s.doWork( connection -> {
				try (Statement st = connection.createStatement()) {
					try (ResultSet rs = st.executeQuery(
							"select createdOn from Person" )) {
						while ( rs.next() ) {
							Time time = rs.getTime( 1 );
							Time offsetTime = Time.valueOf( OffsetTime.ofInstant(
									Instant.ofEpochMilli( 0 ),
									TIME_ZONE.toZoneId()
							).toLocalTime() );
							assertEquals( offsetTime, time );
						}
					}
				}
			} );
			Person person = s.find( Person.class, 1L );
			assertEquals(
					0,
					person.createdOn.getTime() % TimeUnit.DAYS.toSeconds( 1 )
			);
		} );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private Time createdOn = new Time( 0 );
	}
}

