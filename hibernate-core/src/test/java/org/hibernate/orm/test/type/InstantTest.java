/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.TimeZoneSupport;

import org.junit.runners.Parameterized;

/**
 * Tests for storage of Instant properties.
 */
public class InstantTest extends AbstractJavaTimeTypeTest<Instant, InstantTest.EntityWithInstant> {

	private static class ParametersBuilder extends AbstractParametersBuilder<ParametersBuilder> {
		public ParametersBuilder add(int year, int month, int day,
				int hour, int minute, int second, int nanosecond, ZoneId defaultTimeZone) {
			if ( !isNanosecondPrecisionSupported() ) {
				nanosecond = 0;
			}
			return add( defaultTimeZone, year, month, day, hour, minute, second, nanosecond );
		}
	}

	@Parameterized.Parameters(name = "{1}-{2}-{3}T{4}:{5}:{6}.{7}Z {0}")
	public static List<Object[]> data() {
		return new ParametersBuilder()
				// Not affected by any known bug
				.add( 2017, 11, 6, 19, 19, 1, 0, ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 500, ZONE_PARIS )
				.skippedForDialects(
						// MySQL/Mariadb cannot store values equal to epoch exactly, or less, in a timestamp.
						Arrays.asList( MySQLDialect.class, MariaDBDialect.class ),
						b -> b
								.add( 1970, 1, 1, 0, 0, 0, 0, ZONE_GMT )
								.add( 1900, 1, 1, 0, 0, 0, 0, ZONE_GMT )
								.add( 1900, 1, 1, 0, 0, 0, 0, ZONE_OSLO )
								.add( 1900, 1, 1, 0, 0, 0, 0, ZONE_PARIS )
								.add( 1900, 1, 2, 0, 9, 21, 0, ZONE_PARIS )
								.add( 1900, 1, 1, 0, 0, 0, 0, ZONE_AMSTERDAM )
								.add( 1900, 1, 2, 0, 19, 32, 0, ZONE_AMSTERDAM )
								.add( 1899, 12, 31, 23, 59, 59, 999_999_999, ZONE_PARIS )
								.add( 1899, 12, 31, 23, 59, 59, 999_999_999, ZONE_AMSTERDAM )
				)
				.skippedForDialects(
						// MySQL/Mariadb/Sybase cannot store dates in 1600 in a timestamp.
						dialect -> dialect instanceof MySQLDialect || dialect instanceof MariaDBDialect
								|| dialect instanceof SybaseDialect
								|| dialect instanceof H2Dialect && ( (H2Dialect) dialect ).hasOddDstBehavior(),
						b -> b
								.add( 1600, 1, 1, 0, 0, 0, 0, ZONE_AMSTERDAM )
								// Affected by HHH-13266 (JDK-8061577)
								.add( 1892, 1, 1, 0, 0, 0, 0, ZONE_OSLO )
				)
				// HHH-13379: DST end (where Timestamp becomes ambiguous, see JDK-4312621)
				// => This used to work correctly in 5.4.1.Final and earlier
				.skippedForDialects(
						dialect -> dialect instanceof H2Dialect && ( (H2Dialect) dialect ).hasOddDstBehavior(),
						b -> b.add( 2018, 10, 28, 1, 0, 0, 0, ZONE_PARIS )
								.add( 2018, 3, 31, 14, 0, 0, 0, ZONE_AUCKLAND )
				)
				// => This has never worked correctly, unless the JDBC timezone was set to UTC
				.withForcedJdbcTimezone( "UTC", b -> b
						.add( 2018, 10, 28, 0, 0, 0, 0, ZONE_PARIS )
						.add( 2018, 3, 31, 13, 0, 0, 0, ZONE_AUCKLAND )
				)
				// => Also test DST start, just in case
				.add( 2018, 3, 25, 1, 0, 0, 0, ZONE_PARIS )
				.skippedForDialects(
						// No idea what Sybase is doing here exactly
						dialect -> dialect instanceof SybaseASEDialect,
						b -> b.add( 2018, 3, 25, 2, 0, 0, 0, ZONE_PARIS )
								.add( 2018, 9, 30, 2, 0, 0, 0, ZONE_AUCKLAND )
				)
				.add( 2018, 9, 30, 3, 0, 0, 0, ZONE_AUCKLAND )
				// => Also test dates around 1905-01-01, because the code behaves differently before and after 1905
				.add( 1904, 12, 31, 22, 59, 59, 999_999_999, ZONE_PARIS )
				.add( 1904, 12, 31, 23, 59, 59, 999_999_999, ZONE_PARIS )
				.add( 1905, 1, 1, 0, 59, 59, 999_999_999, ZONE_PARIS )
				.add( 1904, 12, 31, 23, 0, 0, 0, ZONE_PARIS )
				.add( 1905, 1, 1, 0, 0, 0, 0, ZONE_PARIS )
				.add( 1905, 1, 1, 1, 0, 0, 0, ZONE_PARIS )
				.build();
	}

	private final int year;
	private final int month;
	private final int day;
	private final int hour;
	private final int minute;
	private final int second;
	private final int nanosecond;

	public InstantTest(EnvironmentParameters env, int year, int month, int day,
			int hour, int minute, int second, int nanosecond) {
		super( env );
		this.year = year;
		this.month = month;
		this.day = day;
		this.hour = hour;
		this.minute = minute;
		this.second = second;
		this.nanosecond = nanosecond;
	}

	@Override
	protected Class<EntityWithInstant> getEntityType() {
		return EntityWithInstant.class;
	}

	@Override
	protected EntityWithInstant createEntityForHibernateWrite(int id) {
		return new EntityWithInstant( id, getExpectedPropertyValueAfterHibernateRead() );
	}

	@Override
	protected Instant getExpectedPropertyValueAfterHibernateRead() {
		return OffsetDateTime.of( year, month, day, hour, minute, second, nanosecond, ZoneOffset.UTC ).toInstant();
	}

	@Override
	protected Instant getActualPropertyValue(EntityWithInstant entity) {
		return entity.value;
	}

	@Override
	protected void setJdbcValueForNonHibernateWrite(PreparedStatement statement, int parameterIndex) throws SQLException {
		if ( sessionFactory().getJdbcServices().getDialect().getTimeZoneSupport() == TimeZoneSupport.NATIVE ) {
			// Oracle and H2 require reading/writing through OffsetDateTime to avoid TZ related miscalculations
			statement.setObject( parameterIndex, getExpectedJdbcValueAfterHibernateWrite().toInstant().atOffset( ZoneOffset.UTC ) );
		}
		else {
			statement.setTimestamp(
					parameterIndex,
					getExpectedJdbcValueAfterHibernateWrite(),
					Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) )
			);
		}
	}

	@Override
	protected Timestamp getExpectedJdbcValueAfterHibernateWrite() {
		return Timestamp.from( getExpectedPropertyValueAfterHibernateRead() );
	}

	@Override
	protected Object getActualJdbcValue(ResultSet resultSet, int columnIndex) throws SQLException {
		if ( sessionFactory().getJdbcServices().getDialect().getTimeZoneSupport() == TimeZoneSupport.NATIVE ) {
			// Oracle and H2 require reading/writing through OffsetDateTime to avoid TZ related miscalculations
			return Timestamp.from( resultSet.getObject( columnIndex, OffsetDateTime.class ).toInstant() );
		}
		else {
			return resultSet.getTimestamp( columnIndex, Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ) );
		}
	}

	@Entity(name = ENTITY_NAME)
	static final class EntityWithInstant {
		@Id
		@Column(name = ID_COLUMN_NAME)
		private Integer id;

		@Basic
		@Column(name = PROPERTY_COLUMN_NAME)
		private Instant value;

		protected EntityWithInstant() {
		}

		private EntityWithInstant(int id, Instant value) {
			this.id = id;
			this.value = value;
		}
	}
}
