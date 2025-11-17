/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.temporal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import jakarta.persistence.Table;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.H2Dialect;
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

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;
import static org.hibernate.cfg.MappingSettings.TIMEZONE_DEFAULT_STORAGE;
import static org.hibernate.orm.test.type.temporal.Timezones.ZONE_AMSTERDAM;
import static org.hibernate.orm.test.type.temporal.Timezones.ZONE_AUCKLAND;
import static org.hibernate.orm.test.type.temporal.Timezones.ZONE_GMT;
import static org.hibernate.orm.test.type.temporal.Timezones.ZONE_OSLO;
import static org.hibernate.orm.test.type.temporal.Timezones.ZONE_PARIS;
import static org.hibernate.orm.test.type.temporal.Timezones.ZONE_UTC_MINUS_8;

/**
 * Tests for storage of OffsetTime properties.
 */
@TestInstance( TestInstance.Lifecycle.PER_METHOD )
@ParameterizedClass
@MethodSource("testData")
@DomainModel(annotatedClasses = OffsetTimeTest.EntityWithOffsetTime.class)
@SessionFactory
public class OffsetTimeTest extends AbstractJavaTimeTypeTests<OffsetTime, OffsetTimeTest.EntityWithOffsetTime> {

	public static List<Parameter<OffsetTime,DataImpl>> testData() {
		return new ParametersBuilder( DialectContext.getDialect() )
				.alsoTestRemappingsWithH2( TimeAsTimestampRemappingH2Dialect.class )
				// None of these values was affected by HHH-13266 (JDK-8061577)
				.add( 19, 19, 1, 0, "+10:00", ZONE_UTC_MINUS_8 )
				.add( 19, 19, 1, 0, "+01:30", ZONE_UTC_MINUS_8 )
				.add( 19, 19, 1, 0, "-06:00", ZONE_UTC_MINUS_8 )
				.add( 19, 19, 1, 0, "+10:00", ZONE_PARIS )
				.add( 19, 19, 1, 0, "+01:30", ZONE_PARIS )
				.add( 19, 19, 1, 500, "+01:00", ZONE_PARIS )
				.add( 19, 19, 1, 0, "-08:00", ZONE_PARIS )
				.add( 0, 9, 20, 0, "+00:09:21", ZONE_PARIS )
				.add( 0, 19, 31, 0, "+00:19:32", ZONE_PARIS )
				.add( 0, 19, 31, 0, "+00:19:32", ZONE_AMSTERDAM )
				.skippedForDialects(
						// MySQL/Mariadb cannot store values equal to epoch exactly, or less, in a timestamp.
						Arrays.asList( MySQLDialect.class, MariaDBDialect.class ),
						b -> b
								.add( 0, 0, 0, 0, "+01:00", ZONE_GMT )
								.add( 0, 0, 0, 0, "+00:00", ZONE_GMT )
								.add( 0, 0, 0, 0, "-01:00", ZONE_GMT )
								.add( 0, 0, 0, 0, "+00:00", ZONE_OSLO )
								.add( 0, 0, 0, 0, "+00:19:32", ZONE_AMSTERDAM )
								.addPersistedWithoutHibernate( 1892, 1, 1, 0, 0, 0, 0, "+00:00", ZONE_OSLO )
								.addPersistedWithoutHibernate( 1900, 1, 1, 0, 9, 20, 0, "+00:09:21", ZONE_PARIS )
								.addPersistedWithoutHibernate( 1900, 1, 1, 0, 19, 31, 0, "+00:19:32", ZONE_PARIS )
								.addPersistedWithoutHibernate( 1900, 1, 1, 0, 19, 31, 0, "+00:19:32", ZONE_AMSTERDAM )
								.addPersistedWithoutHibernate( 1600, 1, 1, 0, 0, 0, 0, "+00:19:32", ZONE_AMSTERDAM )
				)
				// HHH-13379: DST end (where Timestamp becomes ambiguous, see JDK-4312621)
				// It doesn't seem that any time on 1970-01-01 can be affected by HHH-13379, but we add some tests just in case
				.add( 1, 0, 0, 0, "-01:00", ZONE_PARIS )
				.add( 1, 0, 0, 0, "+00:00", ZONE_PARIS )
				.add( 1, 0, 0, 0, "+01:00", ZONE_PARIS )
				.add( 1, 0, 0, 0, "+02:00", ZONE_PARIS )
				.add( 2, 0, 0, 0, "-01:00", ZONE_PARIS )
				.add( 2, 0, 0, 0, "+00:00", ZONE_PARIS )
				.add( 2, 0, 0, 0, "+01:00", ZONE_PARIS )
				.add( 2, 0, 0, 0, "+02:00", ZONE_PARIS )
				.add( 3, 0, 0, 0, "-01:00", ZONE_PARIS )
				.add( 3, 0, 0, 0, "+00:00", ZONE_PARIS )
				.add( 3, 0, 0, 0, "+01:00", ZONE_PARIS )
				.add( 3, 0, 0, 0, "+02:00", ZONE_PARIS )
				.add( 1, 0, 0, 0, "-01:00", ZONE_AUCKLAND )
				.add( 1, 0, 0, 0, "+00:00", ZONE_AUCKLAND )
				.add( 1, 0, 0, 0, "+01:00", ZONE_AUCKLAND )
				.add( 1, 0, 0, 0, "+02:00", ZONE_AUCKLAND )
				.add( 2, 0, 0, 0, "-01:00", ZONE_AUCKLAND )
				.add( 2, 0, 0, 0, "+00:00", ZONE_AUCKLAND )
				.add( 2, 0, 0, 0, "+01:00", ZONE_AUCKLAND )
				.add( 2, 0, 0, 0, "+02:00", ZONE_AUCKLAND )
				.add( 3, 0, 0, 0, "-01:00", ZONE_AUCKLAND )
				.add( 3, 0, 0, 0, "+00:00", ZONE_AUCKLAND )
				.add( 3, 0, 0, 0, "+01:00", ZONE_AUCKLAND )
				.add( 3, 0, 0, 0, "+02:00", ZONE_AUCKLAND )
				.build();
	}

	private final Parameter<OffsetTime,DataImpl> testParam;

	public OffsetTimeTest(Parameter<OffsetTime,DataImpl> testParam) {
		super( testParam.env() );
		this.testParam = testParam;
	}

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		builder.applySetting( TIMEZONE_DEFAULT_STORAGE, NORMALIZE );
		return super.produceServiceRegistry( builder );
	}

	@Override
	protected Class<EntityWithOffsetTime> getEntityType() {
		return EntityWithOffsetTime.class;
	}

	@Override
	protected EntityWithOffsetTime createEntityForHibernateWrite(int id) {
		return new EntityWithOffsetTime( id, getOriginalPropertyValue() );
	}

	protected OffsetTime getOriginalPropertyValue() {
		return testParam.data().makeValue();
	}

	@Override
	protected OffsetTime getExpectedPropertyValueAfterHibernateRead() {
		// For some reason, the offset is not stored, so the restored values use the offset from the default JVM timezone.
		if ( TimeAsTimestampRemappingH2Dialect.class.equals( testParam.env().remappingDialectClass() ) ) {
			return getOriginalPropertyValue().withOffsetSameInstant( OffsetDateTime.now().getOffset() );
		}
		else {
			// When storing time as java.sql.Time, we only get second precision (not nanosecond)
			return getOriginalPropertyValue().withNano( 0 ).withOffsetSameInstant( OffsetDateTime.now().getOffset() );
		}
	}

	@Override
	protected OffsetTime getActualPropertyValue(EntityWithOffsetTime entity) {
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
	@SkipForDialect(dialectClass = H2Dialect.class,
			reason = "As of version 2.0.202 this seems to be a problem")
	public void writeThenNativeRead(SessionFactoryScope factoryScope) {
		super.writeThenNativeRead( factoryScope );
	}

	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	@Entity
	@Table(name = ENTITY_TBL_NAME)
	public static class EntityWithOffsetTime {
		@Id
		@Column(name = ID_COLUMN_NAME)
		private Integer id;

		@Basic
		@Column(name = VALUE_COLUMN_NAME)
		private OffsetTime value;

		protected EntityWithOffsetTime() {
		}

		private EntityWithOffsetTime(int id, OffsetTime value) {
			this.id = id;
			this.value = value;
		}
	}

	public static class TimeAsTimestampRemappingH2Dialect extends AbstractRemappingH2Dialect {
		public TimeAsTimestampRemappingH2Dialect() {
			super( DialectContext.getDialect(), Types.TIME, Types.TIMESTAMP );
		}
	}

	public record DataImpl(int hour, int minute, int second, int nanosecond, String offset,
						int yearWhenPersistedWithoutHibernate, int monthWhenPersistedWithoutHibernate,
						int dayWhenPersistedWithoutHibernate) implements Data<OffsetTime> {
		@Override
		public OffsetTime makeValue() {
			return OffsetTime.of( hour, minute, second, nanosecond, ZoneOffset.of( offset ) );
		}

		public Timestamp asTimestamp() {
			OffsetTime offsetTime = OffsetTime.of( hour, minute, second, 0, ZoneOffset.of(offset) )
					.withOffsetSameInstant( OffsetDateTime.now().getOffset() );
			return Timestamp.valueOf( offsetTime.atDate( LocalDate.EPOCH ).toLocalDateTime() );

		}

		public Time asTime() {
			OffsetTime offsetTime = OffsetTime.of( hour, minute, second, 0, ZoneOffset.of(offset) )
					.withOffsetSameInstant( OffsetDateTime.now().getOffset() );
			return Time.valueOf( offsetTime.toLocalTime() );
		}
	}

	private static class ParametersBuilder
			extends AbstractParametersBuilder<OffsetTime, DataImpl, ParametersBuilder> {
		protected ParametersBuilder(Dialect dialect) {
			super( dialect );
		}

		public ParametersBuilder add(int hour, int minute, int second, int nanosecond, String offset, ZoneId defaultTimeZone) {
			if ( !isNanosecondPrecisionSupported() ) {
				nanosecond = 0;
			}
			return add( defaultTimeZone, new DataImpl( hour, minute, second, nanosecond, offset, 1970, 1, 1 ) );
		}

		public ParametersBuilder addPersistedWithoutHibernate(
				int yearWhenPersistedWithoutHibernate, int monthWhenPersistedWithoutHibernate, int dayWhenPersistedWithoutHibernate,
				int hour, int minute, int second, int nanosecond, String offset, ZoneId defaultTimeZone) {
			if ( !isNanosecondPrecisionSupported() ) {
				nanosecond = 0;
			}
			return add( defaultTimeZone, new DataImpl(
					hour, minute, second, nanosecond, offset,
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
}
