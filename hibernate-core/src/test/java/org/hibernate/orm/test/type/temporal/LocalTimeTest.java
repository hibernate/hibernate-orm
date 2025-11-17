/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.temporal;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests for storage of LocalTime properties.
 */
@TestInstance( TestInstance.Lifecycle.PER_METHOD )
@ParameterizedClass
@MethodSource("testData")
@DomainModel(annotatedClasses = LocalTimeTest.EntityWithLocalTime.class)
@SessionFactory
@SkipForDialect(dialectClass = H2Dialect.class, reason = "H2 1.4.200 DST bug. See org.hibernate.dialect.H2Dialect.hasDstBug")
public class LocalTimeTest extends AbstractJavaTimeTypeTests<LocalTime, LocalTimeTest.EntityWithLocalTime> {

	protected static List<Parameter<LocalTime, LocalTimeData>> testData() {
		return new ParametersBuilder( DialectContext.getDialect() )
				.alsoTestRemappingsWithH2( TimeAsTimestampRemappingH2Dialect.class )
				// None of these values was affected by HHH-13266 (JDK-8061577)
				.add( 19, 19, 1, 0, Timezones.ZONE_UTC_MINUS_8 )
				.add( 19, 19, 1, 0, Timezones.ZONE_PARIS )
				.add( 19, 19, 1, 500, Timezones.ZONE_PARIS )
				.add( 0, 9, 20, 0, Timezones.ZONE_PARIS )
				.add( 0, 19, 31, 0, Timezones.ZONE_AMSTERDAM )
				.skippedForDialects(
						// MySQL/Mariadb cannot store values equal to epoch exactly, or less, in a timestamp.
						Arrays.asList( MySQLDialect.class, MariaDBDialect.class ),
						b -> b
								.add( 0, 0, 0, 0, Timezones.ZONE_GMT )
								.add( 0, 0, 0, 0, Timezones.ZONE_OSLO )
								.add( 0, 0, 0, 0, Timezones.ZONE_AMSTERDAM )
								.addPersistedWithoutHibernate( 1900, 1, 1, 0, 0, 0, 0, Timezones.ZONE_OSLO )
								.addPersistedWithoutHibernate( 1900, 1, 2, 0, 9, 21, 0, Timezones.ZONE_PARIS )
								.addPersistedWithoutHibernate( 1900, 1, 2, 0, 19, 32, 0, Timezones.ZONE_AMSTERDAM )
								.addPersistedWithoutHibernate( 1892, 1, 1, 0, 0, 0, 0, Timezones.ZONE_OSLO )
								.addPersistedWithoutHibernate( 1900, 1, 1, 0, 9, 20, 0, Timezones.ZONE_PARIS )
								.addPersistedWithoutHibernate( 1900, 1, 1, 0, 19, 31, 0, Timezones.ZONE_AMSTERDAM )
								.addPersistedWithoutHibernate( 1600, 1, 1, 0, 0, 0, 0, Timezones.ZONE_AMSTERDAM )
				)
				// HHH-13379: DST end (where Timestamp becomes ambiguous, see JDK-4312621)
				// It doesn't seem that any time on 1970-01-01 can be affected by HHH-13379, but we add some tests just in case
				.add( 1, 0, 0, 0, Timezones.ZONE_PARIS )
				.add( 2, 0, 0, 0, Timezones.ZONE_PARIS )
				.add( 3, 0, 0, 0, Timezones.ZONE_PARIS )
				.add( 1, 0, 0, 0, Timezones.ZONE_AUCKLAND )
				.add( 2, 0, 0, 0, Timezones.ZONE_AUCKLAND )
				.add( 3, 0, 0, 0, Timezones.ZONE_AUCKLAND )
				.build();
	}

	private final Parameter<LocalTime, LocalTimeData> testParam;

	public LocalTimeTest(Parameter<LocalTime, LocalTimeData> testParam) {
		super( testParam.env() );
		this.testParam = testParam;
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
		return testParam.data().makeValue();
	}

	@Override
	protected LocalTime getExpectedPropertyValueAfterHibernateRead() {
		if ( TimeAsTimestampRemappingH2Dialect.class.equals( testParam.env().remappingDialectClass() ) ) {
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
	protected void bindJdbcValue(
			PreparedStatement statement,
			int parameterIndex,
			SessionFactoryScope factoryScope) throws SQLException {
		if ( TimeAsTimestampRemappingH2Dialect.class.equals( testParam.env().remappingDialectClass() ) ) {
			statement.setTimestamp( parameterIndex, testParam.data().asTimestamp() );
		}
		else {
			statement.setTime( parameterIndex, testParam.data().asTime() );
		}
	}

	@Override
	protected Object getExpectedJdbcValueAfterHibernateWrite() {
		if ( TimeAsTimestampRemappingH2Dialect.class.equals( testParam.env().remappingDialectClass() ) ) {
			return testParam.data().asTimestamp();
		}
		else {
			return testParam.data().asTime();
		}
	}

	@Override
	protected Object extractJdbcValue(
			ResultSet resultSet,
			int columnIndex,
			SessionFactoryScope factoryScope) throws SQLException {
		if ( TimeAsTimestampRemappingH2Dialect.class.equals( testParam.env().remappingDialectClass() ) ) {
			return resultSet.getTimestamp( columnIndex );
		}
		else {
			return resultSet.getTime( columnIndex );
		}
	}

	@Override
	@Test
	@SkipForDialect(dialectClass = HANADialect.class,
			reason = "HANA seems to return a java.sql.Timestamp instead of a java.sql.Time")
	@SkipForDialect(dialectClass = MySQLDialect.class,
			reason = "HHH-13580 MySQL seems to store the whole timestamp, not just the time,"
					+ " which for some timezones results in a date other than 1970-01-01 being returned"
					+ " (typically 1969-12-31), even though the time is always right."
					+ " Since java.sql.Time holds the whole timestamp, not just the time,"
					+ " its equals() method ends up returning false in this test.")
	@SkipForDialect(dialectClass = HSQLDialect.class,
			reason = "Timezone issue?")
	public void writeThenNativeRead(SessionFactoryScope factoryScope) {
		super.writeThenNativeRead( factoryScope );
	}

	@Entity
	@Table(name = ENTITY_TBL_NAME)
	public static final class EntityWithLocalTime {
		@Id
		@Column(name = ID_COLUMN_NAME)
		private Integer id;

		@Basic
		@Column(name = VALUE_COLUMN_NAME)
		private LocalTime value;

		protected EntityWithLocalTime() {
		}

		private EntityWithLocalTime(int id, LocalTime value) {
			this.id = id;
			this.value = value;
		}
	}

	private static class ParametersBuilder extends AbstractParametersBuilder<LocalTime, LocalTimeData, ParametersBuilder> {
		public ParametersBuilder(Dialect dialect) {
			super( dialect );
		}

		public ParametersBuilder add(int hour, int minute, int second, int nanosecond, ZoneId defaultTimeZone) {
			if ( !isNanosecondPrecisionSupported() ) {
				nanosecond = 0;
			}
			return add( defaultTimeZone, new LocalTimeData(
					hour,
					minute,
					second,
					nanosecond,
					1970,
					1,
					1
			) );
		}

		public ParametersBuilder addPersistedWithoutHibernate(
				int yearWhenPersistedWithoutHibernate,
				int monthWhenPersistedWithoutHibernate,
				int dayWhenPersistedWithoutHibernate,
				int hour,
				int minute,
				int second,
				int nanosecond,
				ZoneId defaultTimeZone) {
			if ( !isNanosecondPrecisionSupported() ) {
				nanosecond = 0;
			}
			return add( defaultTimeZone, new LocalTimeData(
					hour, minute, second, nanosecond,
					yearWhenPersistedWithoutHibernate, monthWhenPersistedWithoutHibernate, dayWhenPersistedWithoutHibernate
			) );
		}

		@Override
		protected Iterable<? extends ZoneId> getHibernateJdbcTimeZonesToTest() {
			// The MariaDB Connector/J JDBC driver has a bug in ResultSet#getTime(int, Calendar)
			// that prevents our explicit JDBC timezones from being recognized
			// See https://hibernate.atlassian.net/browse/HHH-13581
			// See https://jira.mariadb.org/browse/CONJ-724
			if ( DialectContext.getDialect() instanceof MariaDBDialect ) {
				return Collections.emptySet();
			}
			return super.getHibernateJdbcTimeZonesToTest();
		}
	}

	public static class TimeAsTimestampRemappingH2Dialect extends AbstractRemappingH2Dialect {
		public TimeAsTimestampRemappingH2Dialect(Dialect baseDialect) {
			super( baseDialect, Types.TIME, Types.TIMESTAMP );
		}
	}

	public record LocalTimeData(
			int hour,
			int minute,
			int second,
			int nanosecond,
			int yearWhenPersistedWithoutHibernate,
			int monthWhenPersistedWithoutHibernate,
			int dayWhenPersistedWithoutHibernate) implements Data<LocalTime> {
		@Override
		public LocalTime makeValue() {
			return LocalTime.of( hour(), minute(), second(), nanosecond() );
		}

		public Time asTime() {
			return new Time( hour(), minute(), second() );
		}

		public Timestamp asTimestamp() {
			return new Timestamp(
					yearWhenPersistedWithoutHibernate - 1900,
					monthWhenPersistedWithoutHibernate - 1,
					dayWhenPersistedWithoutHibernate,
					hour, minute, second, nanosecond
			);
		}
	}
}
