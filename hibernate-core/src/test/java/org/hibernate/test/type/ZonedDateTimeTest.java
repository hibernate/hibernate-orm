/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Query;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.type.ZonedDateTimeType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.junit4.CustomParameterized;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
@RunWith(CustomParameterized.class)
@TestForIssue(jiraKey = "HHH-10372")
public class ZonedDateTimeTest extends BaseNonConfigCoreFunctionalTestCase {

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
	 * which is why we take it into account even when testing ZonedDateTime.
	 */
	@Parameterized.Parameters(name = "{0}-{1}-{2}T{3}:{4}:{5}.{6}[{7}] [JVM TZ: {8}]")
	public static List<Object[]> data() {
		DIALECT = determineDialect();
		return Arrays.asList(
				// Not affected by HHH-13266
				data( 2017, 11, 6, 19, 19, 1, 0, "GMT+10:00", ZoneId.of( "UTC-8" ) ),
				data( 2017, 11, 6, 19, 19, 1, 0, "GMT+07:00", ZoneId.of( "UTC-8" ) ),
				data( 2017, 11, 6, 19, 19, 1, 0, "GMT+01:30", ZoneId.of( "UTC-8" ) ),
				data( 2017, 11, 6, 19, 19, 1, 0, "GMT+01:00", ZoneId.of( "UTC-8" ) ),
				data( 2017, 11, 6, 19, 19, 1, 0, "Europe/Paris", ZoneId.of( "UTC-8" ) ),
				data( 2017, 11, 6, 19, 19, 1, 0, "Europe/London", ZoneId.of( "UTC-8" ) ),
				data( 2017, 11, 6, 19, 19, 1, 0, "GMT+00:30", ZoneId.of( "UTC-8" ) ),
				data( 2017, 11, 6, 19, 19, 1, 0, "GMT-02:00", ZoneId.of( "UTC-8" ) ),
				data( 2017, 11, 6, 19, 19, 1, 0, "GMT-06:00", ZoneId.of( "UTC-8" ) ),
				data( 2017, 11, 6, 19, 19, 1, 0, "GMT-08:00", ZoneId.of( "UTC-8" ) ),
				data( 2017, 11, 6, 19, 19, 1, 0, "GMT+10:00", ZoneId.of( "Europe/Paris" ) ),
				data( 2017, 11, 6, 19, 19, 1, 0, "GMT+07:00", ZoneId.of( "Europe/Paris" ) ),
				data( 2017, 11, 6, 19, 19, 1, 0, "GMT+01:30", ZoneId.of( "Europe/Paris" ) ),
				data( 2017, 11, 6, 19, 19, 1, 0, "GMT+01:00", ZoneId.of( "Europe/Paris" ) ),
				data( 2017, 11, 6, 19, 19, 1, 500, "GMT+01:00", ZoneId.of( "Europe/Paris" ) ),
				data( 2017, 11, 6, 19, 19, 1, 0, "Europe/Paris", ZoneId.of( "Europe/Paris" ) ),
				data( 2017, 11, 6, 19, 19, 1, 0, "Europe/London", ZoneId.of( "Europe/Paris" ) ),
				data( 2017, 11, 6, 19, 19, 1, 0, "GMT+00:30", ZoneId.of( "Europe/Paris" ) ),
				data( 2017, 11, 6, 19, 19, 1, 0, "GMT-02:00", ZoneId.of( "Europe/Paris" ) ),
				data( 2017, 11, 6, 19, 19, 1, 0, "GMT-06:00", ZoneId.of( "Europe/Paris" ) ),
				data( 2017, 11, 6, 19, 19, 1, 0, "GMT-08:00", ZoneId.of( "Europe/Paris" ) ),
				data( 1970, 1, 1, 0, 0, 0, 0, "GMT+01:00", ZoneId.of( "GMT" ) ),
				data( 1970, 1, 1, 0, 0, 0, 0, "GMT+00:00", ZoneId.of( "GMT" ) ),
				data( 1970, 1, 1, 0, 0, 0, 0, "GMT-01:00", ZoneId.of( "GMT" ) ),
				data( 1900, 1, 1, 0, 0, 0, 0, "GMT+01:00", ZoneId.of( "GMT" ) ),
				data( 1900, 1, 1, 0, 0, 0, 0, "GMT+00:00", ZoneId.of( "GMT" ) ),
				data( 1900, 1, 1, 0, 0, 0, 0, "GMT-01:00", ZoneId.of( "GMT" ) ),
				data( 1900, 1, 1, 0, 0, 0, 0, "GMT+00:00", ZoneId.of( "Europe/Oslo" ) ),
				data( 1900, 1, 1, 0, 9, 21, 0, "GMT+00:09:21", ZoneId.of( "Europe/Paris" ) ),
				data( 1900, 1, 1, 0, 9, 21, 0, "Europe/Paris", ZoneId.of( "Europe/Paris" ) ),
				data( 1900, 1, 1, 0, 19, 31, 0, "Europe/Paris", ZoneId.of( "Europe/Paris" ) ),
				data( 1900, 1, 1, 0, 19, 32, 0, "GMT+00:19:32", ZoneId.of( "Europe/Paris" ) ),
				data( 1900, 1, 1, 0, 19, 32, 0, "Europe/Amsterdam", ZoneId.of( "Europe/Paris" ) ),
				data( 1900, 1, 1, 0, 19, 32, 0, "GMT+00:19:32", ZoneId.of( "Europe/Amsterdam" ) ),
				data( 1900, 1, 1, 0, 19, 32, 0, "Europe/Amsterdam", ZoneId.of( "Europe/Amsterdam" ) ),
				// Affected by HHH-13266
				data( 1892, 1, 1, 0, 0, 0, 0, "GMT+00:00", ZoneId.of( "Europe/Oslo" ) ),
				data( 1892, 1, 1, 0, 0, 0, 0, "Europe/Oslo", ZoneId.of( "Europe/Oslo" ) ),
				data( 1900, 1, 1, 0, 9, 20, 0, "GMT+00:09:21", ZoneId.of( "Europe/Paris" ) ),
				data( 1900, 1, 1, 0, 9, 20, 0, "Europe/Paris", ZoneId.of( "Europe/Paris" ) ),
				data( 1900, 1, 1, 0, 19, 31, 0, "GMT+00:19:32", ZoneId.of( "Europe/Paris" ) ),
				data( 1900, 1, 1, 0, 19, 31, 0, "GMT+00:19:32", ZoneId.of( "Europe/Amsterdam" ) ),
				data( 1900, 1, 1, 0, 19, 31, 0, "Europe/Amsterdam", ZoneId.of( "Europe/Amsterdam" ) ),
				data( 1600, 1, 1, 0, 0, 0, 0, "GMT+00:19:32", ZoneId.of( "Europe/Amsterdam" ) ),
				data( 1600, 1, 1, 0, 0, 0, 0, "Europe/Amsterdam", ZoneId.of( "Europe/Amsterdam" ) )
		);
	}

	private static Object[] data(int year, int month, int day,
			int hour, int minute, int second, int nanosecond, String zone, ZoneId defaultTimeZone) {
		if ( DIALECT instanceof PostgreSQL81Dialect ) {
			// PostgreSQL apparently doesn't support nanosecond precision correctly
			nanosecond = 0;
		}
		return new Object[] { year, month, day, hour, minute, second, nanosecond, zone, defaultTimeZone };
	}

	private final int year;
	private final int month;
	private final int day;
	private final int hour;
	private final int minute;
	private final int second;
	private final int nanosecond;
	private final String zone;
	private final ZoneId defaultTimeZone;

	public ZonedDateTimeTest(int year, int month, int day,
			int hour, int minute, int second, int nanosecond, String zone, ZoneId defaultTimeZone) {
		this.year = year;
		this.month = month;
		this.day = day;
		this.hour = hour;
		this.minute = minute;
		this.second = second;
		this.nanosecond = nanosecond;
		this.zone = zone;
		this.defaultTimeZone = defaultTimeZone;
	}

	private ZonedDateTime getOriginalZonedDateTime() {
		return ZonedDateTime.of( year, month, day, hour, minute, second, nanosecond, ZoneId.of( zone ) );
	}

	private ZonedDateTime getExpectedZonedDateTime() {
		return getOriginalZonedDateTime().withZoneSameInstant( ZoneId.systemDefault() );
	}

	private Timestamp getExpectedTimestamp() {
		LocalDateTime dateTimeInDefaultTimeZone = getOriginalZonedDateTime().withZoneSameInstant( ZoneId.systemDefault() )
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
		return new Class[] { EntityWithZonedDateTime.class };
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
				session.persist( new EntityWithZonedDateTime( 1, getOriginalZonedDateTime() ) );
			} );
			inTransaction( session -> {
				ZonedDateTime read = session.find( ZonedDateTimeTest.EntityWithZonedDateTime.class, 1 ).value;
				assertEquals(
						"Writing then reading a value should return the original value",
						getExpectedZonedDateTime(), read
				);
				assertTrue(
						getExpectedZonedDateTime().isEqual( read )
				);
				assertEquals(
						0,
						ZonedDateTimeType.INSTANCE.getComparator().compare( getExpectedZonedDateTime(), read )
				);
			} );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13266")
	public void writeThenNativeRead() {
		withDefaultTimeZone( defaultTimeZone, () -> {
			inTransaction( session -> {
				session.persist( new EntityWithZonedDateTime( 1, getOriginalZonedDateTime() ) );
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
							"Raw values written in database should match the original value (same instant)",
							getExpectedTimestamp(),
							nativeRead
					);
				} );
			} );
		} );
	}

	@Test
	public void testRetrievingEntityByZonedDateTime() {
		withDefaultTimeZone( defaultTimeZone, () -> {
			inTransaction( session -> {
				session.persist( new EntityWithZonedDateTime( 1, getOriginalZonedDateTime() ) );
			} );
			Consumer<ZonedDateTime> checkOneMatch = expected -> inSession( s -> {
				Query query = s.createQuery( "from EntityWithZonedDateTime o where o.value = :date" );
				query.setParameter( "date", expected, ZonedDateTimeType.INSTANCE );
				List<EntityWithZonedDateTime> list = query.list();
				assertThat( list.size(), is( 1 ) );
			} );
			checkOneMatch.accept( getOriginalZonedDateTime() );
			checkOneMatch.accept( getExpectedZonedDateTime() );
			checkOneMatch.accept( getExpectedZonedDateTime().withZoneSameInstant( ZoneOffset.UTC ) );
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

	@Entity(name = "EntityWithZonedDateTime")
	@Table(name = "theentity")
	private static final class EntityWithZonedDateTime {
		@Id
		@Column(name = "theid")
		private Integer id;

		@Basic
		@Column(name = "thevalue")
		private ZonedDateTime value;

		protected EntityWithZonedDateTime() {
		}

		private EntityWithZonedDateTime(int id, ZonedDateTime value) {
			this.id = id;
			this.value = value;
		}
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}
}
