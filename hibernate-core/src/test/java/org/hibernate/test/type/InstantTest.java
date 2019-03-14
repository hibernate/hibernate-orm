/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;

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
				// Not affected by HHH-13266 (JDK-8061577)
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
								// Affected by HHH-13266 (JDK-8061577)
								.add( 1892, 1, 1, 0, 0, 0, 0, ZONE_OSLO )
								.add( 1899, 12, 31, 23, 59, 59, 999_999_999, ZONE_PARIS )
								.add( 1899, 12, 31, 23, 59, 59, 999_999_999, ZONE_AMSTERDAM )
								.add( 1600, 1, 1, 0, 0, 0, 0, ZONE_AMSTERDAM )
				)
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
		statement.setTimestamp( parameterIndex, getExpectedJdbcValueAfterHibernateWrite() );
	}

	@Override
	protected Timestamp getExpectedJdbcValueAfterHibernateWrite() {
		LocalDateTime dateTimeInDefaultTimeZone = getExpectedPropertyValueAfterHibernateRead().atZone( ZoneId.systemDefault() )
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
