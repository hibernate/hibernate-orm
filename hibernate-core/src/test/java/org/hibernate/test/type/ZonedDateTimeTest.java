/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Query;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.type.ZonedDateTimeType;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;
import org.junit.runners.Parameterized;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10372")
public class ZonedDateTimeTest extends AbstractJavaTimeTypeTest<ZonedDateTime, ZonedDateTimeTest.EntityWithZonedDateTime> {

	private static class ParametersBuilder extends AbstractParametersBuilder<ParametersBuilder> {
		public ParametersBuilder add(int year, int month, int day,
				int hour, int minute, int second, int nanosecond, String zone, ZoneId defaultTimeZone) {
			if ( !isNanosecondPrecisionSupported() ) {
				nanosecond = 0;
			}
			return add( defaultTimeZone, year, month, day, hour, minute, second, nanosecond, zone );
		}
	}

	@Parameterized.Parameters(name = "{1}-{2}-{3}T{4}:{5}:{6}.{7}[{8}] {0}")
	public static List<Object[]> data() {
		return new ParametersBuilder()
				// Not affected by any known bug
				.add( 2017, 11, 6, 19, 19, 1, 0, "GMT+10:00", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "GMT+07:00", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "GMT+01:30", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "GMT+01:00", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "Europe/Paris", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "Europe/London", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "GMT+00:30", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "GMT-02:00", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "GMT-06:00", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "GMT-08:00", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "GMT+10:00", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "GMT+07:00", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "GMT+01:30", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "GMT+01:00", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 500, "GMT+01:00", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "Europe/Paris", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "Europe/London", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "GMT+00:30", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "GMT-02:00", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "GMT-06:00", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "GMT-08:00", ZONE_PARIS )
				.skippedForDialects(
						// MySQL/Mariadb cannot store values equal to epoch exactly, or less, in a timestamp.
						Arrays.asList( MySQLDialect.class, MariaDBDialect.class ),
						b -> b
								// Not affected by any known bug
								.add( 1970, 1, 1, 0, 0, 0, 0, "GMT+01:00", ZONE_GMT )
								.add( 1970, 1, 1, 0, 0, 0, 0, "GMT+00:00", ZONE_GMT )
								.add( 1970, 1, 1, 0, 0, 0, 0, "GMT-01:00", ZONE_GMT )
								.add( 1900, 1, 1, 0, 0, 0, 0, "GMT+01:00", ZONE_GMT )
								.add( 1900, 1, 1, 0, 0, 0, 0, "GMT+00:00", ZONE_GMT )
								.add( 1900, 1, 1, 0, 0, 0, 0, "GMT-01:00", ZONE_GMT )
								.add( 1900, 1, 1, 0, 0, 0, 0, "GMT+00:00", ZONE_OSLO )
								.add( 1900, 1, 1, 0, 9, 21, 0, "GMT+00:09:21", ZONE_PARIS )
								.add( 1900, 1, 1, 0, 9, 21, 0, "Europe/Paris", ZONE_PARIS )
								.add( 1900, 1, 1, 0, 19, 31, 0, "Europe/Paris", ZONE_PARIS )
								.add( 1900, 1, 1, 0, 19, 32, 0, "GMT+00:19:32", ZONE_PARIS )
								.add( 1900, 1, 1, 0, 19, 32, 0, "Europe/Amsterdam", ZONE_PARIS )
								.add( 1900, 1, 1, 0, 19, 32, 0, "GMT+00:19:32", ZONE_AMSTERDAM )
								.add( 1900, 1, 1, 0, 19, 32, 0, "Europe/Amsterdam", ZONE_AMSTERDAM )
								// Affected by HHH-13266 (JDK-8061577)
								.add( 1892, 1, 1, 0, 0, 0, 0, "GMT+00:00", ZONE_OSLO )
								.add( 1892, 1, 1, 0, 0, 0, 0, "Europe/Oslo", ZONE_OSLO )
								.add( 1900, 1, 1, 0, 9, 20, 0, "GMT+00:09:21", ZONE_PARIS )
								.add( 1900, 1, 1, 0, 9, 20, 0, "Europe/Paris", ZONE_PARIS )
								.add( 1900, 1, 1, 0, 19, 31, 0, "GMT+00:19:32", ZONE_PARIS )
								.add( 1900, 1, 1, 0, 19, 31, 0, "GMT+00:19:32", ZONE_AMSTERDAM )
								.add( 1900, 1, 1, 0, 19, 31, 0, "Europe/Amsterdam", ZONE_AMSTERDAM )
				)
				.skippedForDialects(
						// MySQL/Mariadb/Sybase cannot store dates in 1600 in a timestamp.
						Arrays.asList( MySQLDialect.class, MariaDBDialect.class, SybaseDialect.class ),
						b -> b
								.add( 1600, 1, 1, 0, 0, 0, 0, "GMT+00:19:32", ZONE_AMSTERDAM )
								.add( 1600, 1, 1, 0, 0, 0, 0, "Europe/Amsterdam", ZONE_AMSTERDAM )
				)
				// HHH-13379: DST end (where Timestamp becomes ambiguous, see JDK-4312621)
				// => This used to work correctly in 5.4.1.Final and earlier
				.add( 2018, 10, 28, 2, 0, 0, 0, "+01:00", ZONE_PARIS )
				.add( 2018, 4, 1, 2, 0, 0, 0, "+12:00", ZONE_AUCKLAND )
				// => This has never worked correctly, unless the JDBC timezone was set to UTC
				.withForcedJdbcTimezone( "UTC", b -> b
						.add( 2018, 10, 28, 2, 0, 0, 0, "+02:00", ZONE_PARIS )
						.add( 2018, 4, 1, 2, 0, 0, 0, "+13:00", ZONE_AUCKLAND )
				)
				// => Also test DST start, just in case
				.add( 2018, 3, 25, 2, 0, 0, 0, "+01:00", ZONE_PARIS )
				.add( 2018, 3, 25, 3, 0, 0, 0, "+02:00", ZONE_PARIS )
				.add( 2018, 9, 30, 2, 0, 0, 0, "+12:00", ZONE_AUCKLAND )
				.add( 2018, 9, 30, 3, 0, 0, 0, "+13:00", ZONE_AUCKLAND )
				// => Also test dates around 1905-01-01, because the code behaves differently before and after 1905
				.add( 1904, 12, 31, 23, 59, 59, 999_999_999, "-01:00", ZONE_PARIS )
				.add( 1904, 12, 31, 23, 59, 59, 999_999_999, "+00:00", ZONE_PARIS )
				.add( 1904, 12, 31, 23, 59, 59, 999_999_999, "+01:00", ZONE_PARIS )
				.add( 1905, 1, 1, 0, 0, 0, 0, "-01:00", ZONE_PARIS )
				.add( 1905, 1, 1, 0, 0, 0, 0, "+00:00", ZONE_PARIS )
				.add( 1905, 1, 1, 0, 0, 0, 0, "+01:00", ZONE_PARIS )
				.build();
	}

	private final int year;
	private final int month;
	private final int day;
	private final int hour;
	private final int minute;
	private final int second;
	private final int nanosecond;
	private final String zone;

	public ZonedDateTimeTest(EnvironmentParameters env, int year, int month, int day,
			int hour, int minute, int second, int nanosecond, String zone) {
		super( env );
		this.year = year;
		this.month = month;
		this.day = day;
		this.hour = hour;
		this.minute = minute;
		this.second = second;
		this.nanosecond = nanosecond;
		this.zone = zone;
	}

	@Override
	protected Class<EntityWithZonedDateTime> getEntityType() {
		return EntityWithZonedDateTime.class;
	}

	@Override
	protected EntityWithZonedDateTime createEntityForHibernateWrite(int id) {
		return new EntityWithZonedDateTime(
				id,
				getOriginalZonedDateTime()
		);
	}

	private ZonedDateTime getOriginalZonedDateTime() {
		return ZonedDateTime.of( year, month, day, hour, minute, second, nanosecond, ZoneId.of( zone ) );
	}

	@Override
	protected ZonedDateTime getExpectedPropertyValueAfterHibernateRead() {
		return getOriginalZonedDateTime().withZoneSameInstant( ZoneId.systemDefault() );
	}

	@Override
	protected ZonedDateTime getActualPropertyValue(EntityWithZonedDateTime entity) {
		return entity.value;
	}

	@Override
	protected void setJdbcValueForNonHibernateWrite(PreparedStatement statement, int parameterIndex) throws SQLException {
		statement.setTimestamp( parameterIndex, getExpectedJdbcValueAfterHibernateWrite() );
	}

	@Override
	protected Timestamp getExpectedJdbcValueAfterHibernateWrite() {
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
	protected Object getActualJdbcValue(ResultSet resultSet, int columnIndex) throws SQLException {
		return resultSet.getTimestamp( columnIndex );
	}

	@Test
	public void testRetrievingEntityByZonedDateTime() {
		withDefaultTimeZone( () -> {
			inTransaction( session -> {
				session.persist( new EntityWithZonedDateTime( 1, getOriginalZonedDateTime() ) );
			} );
			Consumer<ZonedDateTime> checkOneMatch = expected -> inSession( s -> {
				Query query = s.createQuery( "from " + ENTITY_NAME + " o where o.value = :date" );
				query.setParameter( "date", expected, ZonedDateTimeType.INSTANCE );
				List<EntityWithZonedDateTime> list = query.list();
				assertThat( list.size(), is( 1 ) );
			} );
			checkOneMatch.accept( getOriginalZonedDateTime() );
			checkOneMatch.accept( getExpectedPropertyValueAfterHibernateRead() );
			checkOneMatch.accept( getExpectedPropertyValueAfterHibernateRead().withZoneSameInstant( ZoneOffset.UTC ) );
		} );
	}

	@Entity(name = ENTITY_NAME)
	static final class EntityWithZonedDateTime {
		@Id
		@Column(name = ID_COLUMN_NAME)
		private Integer id;

		@Basic
		@Column(name = PROPERTY_COLUMN_NAME)
		private ZonedDateTime value;

		protected EntityWithZonedDateTime() {
		}

		private EntityWithZonedDateTime(int id, ZonedDateTime value) {
			this.id = id;
			this.value = value;
		}
	}
}
