/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.temporal;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import jakarta.persistence.Table;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.SybaseASEDialect;

import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hibernate.orm.test.type.temporal.Timezones.ZONE_AMSTERDAM;
import static org.hibernate.orm.test.type.temporal.Timezones.ZONE_AUCKLAND;
import static org.hibernate.orm.test.type.temporal.Timezones.ZONE_GMT;
import static org.hibernate.orm.test.type.temporal.Timezones.ZONE_OSLO;
import static org.hibernate.orm.test.type.temporal.Timezones.ZONE_PARIS;
import static org.hibernate.orm.test.type.temporal.Timezones.ZONE_SANTIAGO;
import static org.hibernate.orm.test.type.temporal.Timezones.ZONE_UTC_MINUS_8;

/**
 * Tests for storage of LocalDate properties.
 */
@JiraKey(value = "HHH-10371")
@TestInstance( TestInstance.Lifecycle.PER_METHOD )
@ParameterizedClass
@MethodSource("testData")
@DomainModel(annotatedClasses = LocalDateTest.EntityWithLocalDate.class)
@SessionFactory
@SkipForDialect(dialectClass = HANADialect.class,
		reason = "HANA systematically returns the wrong date when the JVM default timezone is not UTC")
@SkipForDialect(dialectClass = MySQLDialect.class,
		reason = "HHH-13582: MySQL ConnectorJ 8.x returns the wrong date"
				+ " when the JVM default timezone is different from the server timezone:"
				+ " https://bugs.mysql.com/bug.php?id=91112"
)
@SkipForDialect(dialectClass = H2Dialect.class,
		reason = "H2 1.4.200 DST bug. See org.hibernate.dialect.H2Dialect.hasDstBug")
@SkipForDialect(dialectClass = HSQLDialect.class,
		reason = "HSQL has problems with DST edges")
public class LocalDateTest extends AbstractJavaTimeTypeTests<LocalDate, LocalDateTest.EntityWithLocalDate> {

	public static List<Parameter<LocalDate, DataImpl>> testData() {
		return new ParametersBuilder( DialectContext.getDialect() )
				.alsoTestRemappingsWithH2( DateAsTimestampRemappingH2Dialect.class )
				// Not affected by HHH-13266 (JDK-8061577)
				.add( 2017, 11, 6, ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, ZONE_PARIS )
				.skippedForDialects(
						// MySQL/Mariadb cannot store values equal to epoch exactly, or less, in a timestamp.
						Arrays.asList( MySQLDialect.class, MariaDBDialect.class ),
						b -> b
								.add( 1970, 1, 1, ZONE_GMT )
								.add( 1900, 1, 1, ZONE_GMT )
								.add( 1900, 1, 1, ZONE_OSLO )
								.add( 1900, 1, 2, ZONE_PARIS )
								.add( 1900, 1, 2, ZONE_AMSTERDAM )
								// Affected by HHH-13266 (JDK-8061577), but only when remapping dates as timestamps
								.add( 1892, 1, 1, ZONE_OSLO )
								.add( 1900, 1, 1, ZONE_PARIS )
								.add( 1900, 1, 1, ZONE_AMSTERDAM )
				)
				.skippedForDialects(
						// No idea what Sybase is doing here exactly
						dialect -> dialect instanceof SybaseASEDialect,
						b -> b.add( 1600, 1, 1, ZONE_AMSTERDAM )
				)
				// HHH-13379: DST end (where Timestamp becomes ambiguous, see JDK-4312621)
				// It doesn't seem that any date at midnight can be affected by HHH-13379, but we add some tests just in case
				// => Test the day of DST end
				.add( 2018, 10, 28, ZONE_PARIS )
				.add( 2018, 9, 30, ZONE_AUCKLAND )
				.add( 2018, 5, 13, ZONE_SANTIAGO ) // DST end: 00:00 => 23:00 previous day
				// => Also test the day of DST start
				.add( 2018, 3, 25, ZONE_PARIS )
				.add( 2018, 9, 30, ZONE_AUCKLAND )
				.add( 2018, 8, 12, ZONE_SANTIAGO ) // DST start: 00:00 => 01:00
				.build();
	}

	private final Parameter<LocalDate, DataImpl> testParam;

	public LocalDateTest(Parameter<LocalDate, DataImpl> testParam) {
		super( testParam.env() );
		this.testParam = testParam;
	}

	@Override
	protected Class<EntityWithLocalDate> getEntityType() {
		return EntityWithLocalDate.class;
	}

	@Override
	protected EntityWithLocalDate createEntityForHibernateWrite(int id) {
		return new EntityWithLocalDate( id, getExpectedPropertyValueAfterHibernateRead() );
	}

	@Override
	protected LocalDate getExpectedPropertyValueAfterHibernateRead() {
		return testParam.data().makeValue();
	}

	@Override
	protected LocalDate getActualPropertyValue(EntityWithLocalDate entity) {
		return entity.value;
	}

	@Override
	protected void bindJdbcValue(
			PreparedStatement statement,
			int parameterIndex,
			SessionFactoryScope factoryScope) throws SQLException {
		if ( DateAsTimestampRemappingH2Dialect.class.equals( testParam.env().remappingDialectClass() ) ) {
			statement.setTimestamp( parameterIndex, testParam.data().asTimestamp() );
		}
		else {
			statement.setDate( parameterIndex, testParam.data().asDate() );
		}
	}

	@Override
	protected Object getExpectedJdbcValueAfterHibernateWrite() {
		if ( DateAsTimestampRemappingH2Dialect.class.equals( testParam.env().remappingDialectClass() ) ) {
			return testParam.data().asTimestamp();
		}
		else {
			return testParam.data().asDate();
		}
	}

	@Override
	protected Object extractJdbcValue(
			ResultSet resultSet,
			int columnIndex,
			SessionFactoryScope factoryScope) throws SQLException {
		if ( DateAsTimestampRemappingH2Dialect.class.equals( testParam.env().remappingDialectClass() ) ) {
			return resultSet.getTimestamp( columnIndex );
		}
		else {
			return resultSet.getDate( columnIndex );
		}
	}

	@SuppressWarnings({"unused", "FieldCanBeLocal"})
	@Entity
	@Table(name = ENTITY_TBL_NAME)
	public static class EntityWithLocalDate {
		@Id
		@Column(name = ID_COLUMN_NAME)
		private Integer id;

		@Basic
		@Column(name = VALUE_COLUMN_NAME)
		private LocalDate value;

		protected EntityWithLocalDate() {
		}

		private EntityWithLocalDate(int id, LocalDate value) {
			this.id = id;
			this.value = value;
		}
	}

	public static class DateAsTimestampRemappingH2Dialect extends AbstractRemappingH2Dialect {
		public DateAsTimestampRemappingH2Dialect() {
			super( DialectContext.getDialect(), Types.DATE, Types.TIMESTAMP );
		}
	}

	public record DataImpl(int year, int month, int day) implements Data<LocalDate> {
		@Override
		public LocalDate makeValue() {
			return LocalDate.of( year, month, day );
		}

		public Timestamp asTimestamp() {
			return new Timestamp( year - 1900, month - 1, day, 0, 0, 0, 0 );
		}

		public Date asDate() {
			return new Date( year - 1900, month - 1, day );
		}
	}

	private static class ParametersBuilder extends AbstractParametersBuilder<LocalDate, DataImpl, ParametersBuilder> {
		protected ParametersBuilder(Dialect dialect) {
			super( dialect );
		}

		public ParametersBuilder add(int year, int month, int day, ZoneId defaultTimeZone) {
			return add( defaultTimeZone, new DataImpl( year, month, day ) );
		}
	}
}
