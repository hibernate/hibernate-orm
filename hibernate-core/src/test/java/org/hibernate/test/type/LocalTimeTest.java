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
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.type.descriptor.sql.TimestampTypeDescriptor;

import org.hibernate.testing.SkipForDialect;
import org.junit.Test;
import org.junit.runners.Parameterized;

/**
 * Tests for storage of LocalTime properties.
 */
public class LocalTimeTest extends AbstractJavaTimeTypeTest<LocalTime, LocalTimeTest.EntityWithLocalTime> {

	private static class ParametersBuilder extends AbstractParametersBuilder<ParametersBuilder> {
		public ParametersBuilder add(int hour, int minute, int second, int nanosecond, ZoneId defaultTimeZone) {
			if ( !isNanosecondPrecisionSupported() ) {
				nanosecond = 0;
			}
			return add( defaultTimeZone, hour, minute, second, nanosecond, 1970, 1, 1 );
		}

		public ParametersBuilder addPersistedWithoutHibernate(int yearWhenPersistedWithoutHibernate,
				int monthWhenPersistedWithoutHibernate, int dayWhenPersistedWithoutHibernate,
				int hour, int minute, int second, int nanosecond, ZoneId defaultTimeZone) {
			if ( !isNanosecondPrecisionSupported() ) {
				nanosecond = 0;
			}
			return add(
					defaultTimeZone, hour, minute, second, nanosecond,
					yearWhenPersistedWithoutHibernate,
					monthWhenPersistedWithoutHibernate, dayWhenPersistedWithoutHibernate
			);
		}
	}

	@Parameterized.Parameters(name = "{1}:{2}:{3}.{4} (JDBC write date: {5}-{6}-{7}) {0}")
	public static List<Object[]> data() {
		return new ParametersBuilder()
				.alsoTestRemappingsWithH2( TimeAsTimestampRemappingH2Dialect.class )
				// None of these values was affected by HHH-13266 (JDK-8061577)
				.add( 19, 19, 1, 0, ZONE_UTC_MINUS_8 )
				.add( 19, 19, 1, 0, ZONE_PARIS )
				.add( 19, 19, 1, 500, ZONE_PARIS )
				.add( 0, 9, 20, 0, ZONE_PARIS )
				.add( 0, 19, 31, 0, ZONE_AMSTERDAM )
				.skippedForDialects(
						// MySQL/Mariadb cannot store values equal to epoch exactly, or less, in a timestamp.
						Arrays.asList( MySQLDialect.class, MariaDBDialect.class ),
						b -> b
								.add( 0, 0, 0, 0, ZONE_GMT )
								.add( 0, 0, 0, 0, ZONE_OSLO )
								.add( 0, 0, 0, 0, ZONE_AMSTERDAM )
								.addPersistedWithoutHibernate( 1900, 1, 1, 0, 0, 0, 0, ZONE_OSLO )
								.addPersistedWithoutHibernate( 1900, 1, 2, 0, 9, 21, 0, ZONE_PARIS )
								.addPersistedWithoutHibernate( 1900, 1, 2, 0, 19, 32, 0, ZONE_AMSTERDAM )
								.addPersistedWithoutHibernate( 1892, 1, 1, 0, 0, 0, 0, ZONE_OSLO )
								.addPersistedWithoutHibernate( 1900, 1, 1, 0, 9, 20, 0, ZONE_PARIS )
								.addPersistedWithoutHibernate( 1900, 1, 1, 0, 19, 31, 0, ZONE_AMSTERDAM )
								.addPersistedWithoutHibernate( 1600, 1, 1, 0, 0, 0, 0, ZONE_AMSTERDAM )
				)
				.build();
	}

	private final int hour;
	private final int minute;
	private final int second;
	private final int nanosecond;

	private final int yearWhenPersistedWithoutHibernate;
	private final int monthWhenPersistedWithoutHibernate;
	private final int dayWhenPersistedWithoutHibernate;

	public LocalTimeTest(EnvironmentParameters env, int hour, int minute, int second, int nanosecond,
			int yearWhenPersistedWithoutHibernate, int monthWhenPersistedWithoutHibernate, int dayWhenPersistedWithoutHibernate) {
		super( env );
		this.hour = hour;
		this.minute = minute;
		this.second = second;
		this.nanosecond = nanosecond;
		this.yearWhenPersistedWithoutHibernate = yearWhenPersistedWithoutHibernate;
		this.monthWhenPersistedWithoutHibernate = monthWhenPersistedWithoutHibernate;
		this.dayWhenPersistedWithoutHibernate = dayWhenPersistedWithoutHibernate;
	}

	@Override
	protected Class<EntityWithLocalTime> getEntityType() {
		return EntityWithLocalTime.class;
	}

	@Override
	protected EntityWithLocalTime createEntityForHibernateWrite(int id) {
		return new EntityWithLocalTime( id, getOriginalPropertyValue() );
	}

	protected LocalTime getOriginalPropertyValue() {
		return LocalTime.of( hour, minute, second, nanosecond );
	}

	@Override
	protected LocalTime getExpectedPropertyValueAfterHibernateRead() {
		if ( TimeAsTimestampRemappingH2Dialect.class.equals( getRemappingDialectClass() ) ) {
			return getOriginalPropertyValue();
		}
		else {
			// When storing time as java.sql.Time, we only get second precision (not nanosecond)
			return getOriginalPropertyValue().withNano( 0 );
		}
	}

	@Override
	protected LocalTime getActualPropertyValue(EntityWithLocalTime entity) {
		return entity.value;
	}

	@Override
	protected void setJdbcValueForNonHibernateWrite(PreparedStatement statement, int parameterIndex)
			throws SQLException {
		if ( TimeAsTimestampRemappingH2Dialect.class.equals( getRemappingDialectClass() ) ) {
			statement.setTimestamp(
					parameterIndex,
					new Timestamp(
							yearWhenPersistedWithoutHibernate - 1900,
							monthWhenPersistedWithoutHibernate - 1,
							dayWhenPersistedWithoutHibernate,
							hour, minute, second, nanosecond
					)
			);
		}
		else {
			statement.setTime( parameterIndex, new Time( hour, minute, second ) );
		}
	}

	@Override
	protected Object getExpectedJdbcValueAfterHibernateWrite() {
		if ( TimeAsTimestampRemappingH2Dialect.class.equals( getRemappingDialectClass() ) ) {
			return new Timestamp( 1970 - 1900, 0, 1, hour, minute, second, nanosecond );
		}
		else {
			return new Time( hour, minute, second );
		}
	}

	@Override
	protected Object getActualJdbcValue(ResultSet resultSet, int columnIndex) throws SQLException {
		return resultSet.getTimestamp( columnIndex );
	}

	@Override
	@Test
	@SkipForDialect(value = AbstractHANADialect.class, comment = "HANA seems to return a java.sql.Timestamp instead of a java.sql.Time")
	public void writeThenNativeRead() {
		super.writeThenNativeRead();
	}

	@Entity(name = ENTITY_NAME)
	static final class EntityWithLocalTime {
		@Id
		@Column(name = ID_COLUMN_NAME)
		private Integer id;

		@Basic
		@Column(name = PROPERTY_COLUMN_NAME)
		private LocalTime value;

		protected EntityWithLocalTime() {
		}

		private EntityWithLocalTime(int id, LocalTime value) {
			this.id = id;
			this.value = value;
		}
	}

	public static class TimeAsTimestampRemappingH2Dialect extends AbstractRemappingH2Dialect {
		public TimeAsTimestampRemappingH2Dialect() {
			super( Types.TIME, TimestampTypeDescriptor.INSTANCE );
		}
	}
}
