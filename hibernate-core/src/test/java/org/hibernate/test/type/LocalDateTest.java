/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.type;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.ZoneId;
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

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.junit4.CustomParameterized;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

/**
 * Tests for storage of LocalDate properties.
 */
@RunWith(CustomParameterized.class)
@TestForIssue(jiraKey = "HHH-10371")
public class LocalDateTest extends BaseCoreFunctionalTestCase {

	/*
	 * The default timezone affects conversions done using java.util,
	 * which is why we take it into account even when testing LocalDateTime.
	 */
	@Parameterized.Parameters(name = "{0}-{1}-{2} [JVM TZ: {3}]")
	public static List<Object[]> data() {
		return Arrays.asList(
				// Not affected by HHH-13266 (JDK-8061577)
				data( 2017, 11, 6, ZoneId.of( "UTC-8" ) ),
				data( 2017, 11, 6, ZoneId.of( "Europe/Paris" ) ),
				data( 1970, 1, 1, ZoneId.of( "GMT" ) ),
				data( 1900, 1, 1, ZoneId.of( "GMT" ) ),
				data( 1900, 1, 1, ZoneId.of( "Europe/Oslo" ) ),
				data( 1900, 1, 2, ZoneId.of( "Europe/Paris" ) ),
				data( 1900, 1, 2, ZoneId.of( "Europe/Amsterdam" ) ),
				// Could have been affected by HHH-13266 (JDK-8061577), but was not
				data( 1892, 1, 1, ZoneId.of( "Europe/Oslo" ) ),
				data( 1900, 1, 1, ZoneId.of( "Europe/Paris" ) ),
				data( 1900, 1, 1, ZoneId.of( "Europe/Amsterdam" ) ),
				data( 1600, 1, 1, ZoneId.of( "Europe/Amsterdam" ) )
		);
	}

	private static Object[] data(int year, int month, int day, ZoneId defaultTimeZone) {
		return new Object[] { year, month, day, defaultTimeZone };
	}

	private final int year;
	private final int month;
	private final int day;
	private final ZoneId defaultTimeZone;

	public LocalDateTest(int year, int month, int day, ZoneId defaultTimeZone) {
		this.year = year;
		this.month = month;
		this.day = day;
		this.defaultTimeZone = defaultTimeZone;
	}

	private LocalDate getExpectedLocalDate() {
		return LocalDate.of( year, month, day );
	}

	private Date getExpectedSqlDate() {
		return new Date( year - 1900, month - 1, day );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { EntityWithLocalDate.class };
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
				session.persist( new EntityWithLocalDate( 1, getExpectedLocalDate() ) );
			} );
			inTransaction( session -> {
				LocalDate read = session.find( EntityWithLocalDate.class, 1 ).value;
				assertEquals(
						"Writing then reading a value should return the original value",
						getExpectedLocalDate(), read
				);
			} );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13266")
	public void writeThenNativeRead() {
		withDefaultTimeZone( defaultTimeZone, () -> {
			inTransaction( session -> {
				session.persist( new EntityWithLocalDate( 1, getExpectedLocalDate() ) );
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
					Date nativeRead = resultSet.getDate( 1 );
					assertEquals(
							"Raw values written in database should match the original value (same day, hour, ...)",
							getExpectedSqlDate(),
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
	private static final class EntityWithLocalDate {
		@Id
		@Column(name = "theid")
		private Integer id;

		@Basic
		@Column(name = "thevalue")
		private LocalDate value;

		protected EntityWithLocalDate() {
		}

		private EntityWithLocalDate(int id, LocalDate value) {
			this.id = id;
			this.value = value;
		}
	}
}
