/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQL81Dialect;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.junit4.CustomParameterized;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

/**
 * Tests for storage of Instant properties.
 */
@RunWith(CustomParameterized.class)
public class InstantTest extends BaseCoreFunctionalTestCase {

	private static Dialect DIALECT;
	private static Dialect determineDialect() {
		try {
			return Dialect.getDialect();
		}
		catch (Exception e) {
			return new Dialect() {
			};
		}
	}

	/*
	 * The default timezone affects conversions done using java.util,
	 * which is why we take it into account even when testing Instant.
	 */
	@Parameterized.Parameters(name = "{0}-{1}-{2}T{3}:{4}:{5}.{6}Z [JVM TZ: {7}]")
	public static List<Object[]> data() {
		DIALECT = determineDialect();
		return Arrays.asList(
				// Not affected by HHH-13266 (JDK-8061577)
				data( 2017, 11, 6, 19, 19, 1, 0, ZoneId.of( "UTC-8" ) ),
				data( 2017, 11, 6, 19, 19, 1, 0, ZoneId.of( "Europe/Paris" ) ),
				data( 2017, 11, 6, 19, 19, 1, 500, ZoneId.of( "Europe/Paris" ) ),
				data( 1970, 1, 1, 0, 0, 0, 0, ZoneId.of( "GMT" ) ),
				data( 1900, 1, 1, 0, 0, 0, 0, ZoneId.of( "GMT" ) ),
				data( 1900, 1, 1, 0, 0, 0, 0, ZoneId.of( "Europe/Oslo" ) ),
				data( 1900, 1, 1, 0, 0, 0, 0, ZoneId.of( "Europe/Paris" ) ),
				data( 1900, 1, 2, 0, 9, 21, 0, ZoneId.of( "Europe/Paris" ) ),
				data( 1900, 1, 1, 0, 0, 0, 0, ZoneId.of( "Europe/Amsterdam" ) ),
				data( 1900, 1, 2, 0, 19, 32, 0, ZoneId.of( "Europe/Amsterdam" ) ),
				// Affected by HHH-13266 (JDK-8061577)
				data( 1892, 1, 1, 0, 0, 0, 0, ZoneId.of( "Europe/Oslo" ) ),
				data( 1899, 12, 31, 23, 59, 59, 999_999_999, ZoneId.of( "Europe/Paris" ) ),
				data( 1899, 12, 31, 23, 59, 59, 999_999_999, ZoneId.of( "Europe/Amsterdam" ) ),
				data( 1600, 1, 1, 0, 0, 0, 0, ZoneId.of( "Europe/Amsterdam" ) )
		);
	}

	private static Object[] data(int year, int month, int day,
			int hour, int minute, int second, int nanosecond, ZoneId defaultTimeZone) {
		if ( DIALECT instanceof PostgreSQL81Dialect ) {
			// PostgreSQL apparently doesn't support nanosecond precision correctly
			nanosecond = 0;
		}
		return new Object[] { year, month, day, hour, minute, second, nanosecond, defaultTimeZone };
	}

	private final int year;
	private final int month;
	private final int day;
	private final int hour;
	private final int minute;
	private final int second;
	private final int nanosecond;
	private final ZoneId defaultTimeZone;

	public InstantTest(int year, int month, int day,
			int hour, int minute, int second, int nanosecond, ZoneId defaultTimeZone) {
		this.year = year;
		this.month = month;
		this.day = day;
		this.hour = hour;
		this.minute = minute;
		this.second = second;
		this.nanosecond = nanosecond;
		this.defaultTimeZone = defaultTimeZone;
	}

	private Instant getExpectedInstant() {
		return OffsetDateTime.of( year, month, day, hour, minute, second, nanosecond, ZoneOffset.UTC ).toInstant();
	}

	private Timestamp getExpectedTimestamp() {
		LocalDateTime dateTimeInDefaultTimeZone = getExpectedInstant().atZone( ZoneId.systemDefault() )
				.toLocalDateTime();
		return new Timestamp(
				dateTimeInDefaultTimeZone.getYear() - 1900, dateTimeInDefaultTimeZone.getMonthValue() - 1,
				dateTimeInDefaultTimeZone.getDayOfMonth(),
				dateTimeInDefaultTimeZone.getHour(), dateTimeInDefaultTimeZone.getMinute(),
				dateTimeInDefaultTimeZone.getSecond(),
				dateTimeInDefaultTimeZone.getNano()
		);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { EntityWithInstant.class };
	}

	@Before
	public void cleanup() {
		inTransaction( session -> {
			session.createNativeQuery( "DELETE FROM theentity" ).executeUpdate();
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13266")
	public void writeThenRead() {
		withDefaultTimeZone( defaultTimeZone, () -> {
			inTransaction( session -> {
				session.persist( new EntityWithInstant( 1, getExpectedInstant() ) );
			} );
			inTransaction( session -> {
				Instant read = session.find( EntityWithInstant.class, 1 ).value;
				assertEquals(
						"Writing then reading a value should return the original value",
						getExpectedInstant(), read
				);
			} );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13266")
	public void writeThenNativeRead() {
		withDefaultTimeZone( defaultTimeZone, () -> {
			inTransaction( session -> {
				session.persist( new EntityWithInstant( 1, getExpectedInstant() ) );
			} );
			inTransaction( session -> {
				session.doWork( connection -> {
					final PreparedStatement statement = connection.prepareStatement(
							"SELECT thevalue FROM theentity WHERE theid = ?"
					);
					statement.setInt( 1, 1 );
					statement.execute();
					final ResultSet resultSet = statement.getResultSet();
					resultSet.next();
					Timestamp nativeRead = resultSet.getTimestamp( 1 );
					assertEquals(
							"Raw values written in database should match the original value (same day, hour, ...)",
							getExpectedTimestamp(),
							nativeRead
					);
				} );
			} );
		} );
	}

	private static void withDefaultTimeZone(ZoneId zoneId, Runnable runnable) {
		TimeZone timeZoneBefore = TimeZone.getDefault();
		TimeZone.setDefault( TimeZone.getTimeZone( zoneId ) );
		/*
		 * Run the code in a new thread, because some libraries (looking at you, h2 JDBC driver)
		 * cache data dependent on the default timezone in thread local variables,
		 * and we want this data to be reinitialized with the new default time zone.
		 */
		try {
			ExecutorService executor = Executors.newSingleThreadExecutor();
			Future<?> future = executor.submit( runnable );
			executor.shutdown();
			future.get();
		}
		catch (InterruptedException e) {
			throw new IllegalStateException( "Interrupted while testing", e );
		}
		catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if ( cause instanceof RuntimeException ) {
				throw (RuntimeException) cause;
			}
			else if ( cause instanceof Error ) {
				throw (Error) cause;
			}
			else {
				throw new IllegalStateException( "Unexpected exception while testing", cause );
			}
		}
		finally {
			TimeZone.setDefault( timeZoneBefore );
		}
	}

	@Entity
	@Table(name = "theentity")
	private static final class EntityWithInstant {
		@Id
		@Column(name = "theid")
		private Integer id;

		@Basic
		@Column(name = "thevalue")
		private Instant value;

		protected EntityWithInstant() {
		}

		private EntityWithInstant(int id, Instant value) {
			this.id = id;
			this.value = value;
		}
	}
}
