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
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.TimeZoneSupport;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;


/**
 * Tests for storage of Instant properties.
 */
@TestInstance( TestInstance.Lifecycle.PER_METHOD )
@ParameterizedClass
@MethodSource("testData")
@DomainModel(annotatedClasses = InstantTests.EntityWithInstant.class)
@SessionFactory
public class InstantTests extends AbstractJavaTimeTypeTests<Instant, InstantTests.EntityWithInstant> {

	protected static List<Parameter<Instant,InstantData>> testData() {
		return new ParametersBuilder( DialectContext.getDialect() )
				// Not affected by any known bug
				.add( 2017, 11, 6, 19, 19, 1, 0, Timezones.ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, Timezones.ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 500, Timezones.ZONE_PARIS )
				.skippedForDialects(
						// MySQL/Mariadb cannot store values equal to epoch exactly, or less, in a timestamp.
						List.of(MySQLDialect.class),
						(collector) -> collector
								.add( 1970, 1, 1, 0, 0, 0, 0, Timezones.ZONE_GMT )
								.add( 1900, 1, 1, 0, 0, 0, 0, Timezones.ZONE_GMT )
								.add( 1900, 1, 1, 0, 0, 0, 0, Timezones.ZONE_OSLO )
								.add( 1900, 1, 1, 0, 0, 0, 0, Timezones.ZONE_PARIS )
								.add( 1900, 1, 2, 0, 9, 21, 0, Timezones.ZONE_PARIS )
								.add( 1900, 1, 1, 0, 0, 0, 0, Timezones.ZONE_AMSTERDAM )
								.add( 1900, 1, 2, 0, 19, 32, 0, Timezones.ZONE_AMSTERDAM )
								.add( 1899, 12, 31, 23, 59, 59, 999_999_999, Timezones.ZONE_PARIS )
								.add( 1899, 12, 31, 23, 59, 59, 999_999_999, Timezones.ZONE_AMSTERDAM )
				)
				.skippedForDialects(
						// MySQL/Mariadb/Sybase cannot store dates in 1600 in a timestamp.
						dialect -> dialect instanceof MySQLDialect
								|| dialect instanceof SybaseDialect
								|| dialect instanceof H2Dialect && ( (H2Dialect) dialect ).hasOddDstBehavior(),
						b -> b
								.add( 1600, 1, 1, 0, 0, 0, 0, Timezones.ZONE_AMSTERDAM )
								// Affected by HHH-13266 (JDK-8061577)
								.add( 1892, 1, 1, 0, 0, 0, 0, Timezones.ZONE_OSLO )
				)
				// HHH-13379: DST end (where Timestamp becomes ambiguous, see JDK-4312621)
				// => This used to work correctly in 5.4.1.Final and earlier
				.skippedForDialects(
						dialect -> dialect instanceof H2Dialect && ( (H2Dialect) dialect ).hasOddDstBehavior(),
						b -> b.add( 2018, 10, 28, 1, 0, 0, 0, Timezones.ZONE_PARIS )
								.add( 2018, 3, 31, 14, 0, 0, 0, Timezones.ZONE_AUCKLAND )
				)
				// => This has never worked correctly, unless the JDBC timezone was set to UTC
				.withForcedJdbcTimezone( "UTC", b -> b
						.add( 2018, 10, 28, 0, 0, 0, 0, Timezones.ZONE_PARIS )
						.add( 2018, 3, 31, 13, 0, 0, 0, Timezones.ZONE_AUCKLAND )
				)
				// => Also test DST start, just in case
				.add( 2018, 3, 25, 1, 0, 0, 0, Timezones.ZONE_PARIS )
				.skippedForDialects(
						// No idea what Sybase is doing here exactly
						dialect -> dialect instanceof SybaseASEDialect,
						b -> b.add( 2018, 3, 25, 2, 0, 0, 0, Timezones.ZONE_PARIS )
								.add( 2018, 9, 30, 2, 0, 0, 0, Timezones.ZONE_AUCKLAND )
				)
				.add( 2018, 9, 30, 3, 0, 0, 0, Timezones.ZONE_AUCKLAND )
				// => Also test dates around 1905-01-01, because the code behaves differently before and after 1905
				.add( 1904, 12, 31, 22, 59, 59, 999_999_999, Timezones.ZONE_PARIS )
				.add( 1904, 12, 31, 23, 59, 59, 999_999_999, Timezones.ZONE_PARIS )
				.add( 1905, 1, 1, 0, 59, 59, 999_999_999, Timezones.ZONE_PARIS )
				.add( 1904, 12, 31, 23, 0, 0, 0, Timezones.ZONE_PARIS )
				.add( 1905, 1, 1, 0, 0, 0, 0, Timezones.ZONE_PARIS )
				.add( 1905, 1, 1, 1, 0, 0, 0, Timezones.ZONE_PARIS )
				.build();
	}

	private final Parameter<Instant,InstantData> testParam;

	public InstantTests(Parameter<Instant, InstantData> testParam) {
		super( testParam.env() );
		this.testParam = testParam;
	}

	@Override
	protected Class<EntityWithInstant> getEntityType() {
		return EntityWithInstant.class;
	}

	@Override
	protected EntityWithInstant createEntityForHibernateWrite(int id) {
		return new EntityWithInstant( id, testParam.data().makeValue() );
	}

	@Override
	protected Instant getExpectedPropertyValueAfterHibernateRead() {
		return testParam.data().asInstant();
	}

	@Override
	protected Instant getActualPropertyValue(EntityWithInstant entity) {
		return entity.getValue();
	}

	@Override
	protected Timestamp getExpectedJdbcValueAfterHibernateWrite() {
		return Timestamp.from( getExpectedPropertyValueAfterHibernateRead() );
	}

	@Override
	protected Object extractJdbcValue(
			ResultSet resultSet,
			int position,
			SessionFactoryScope factoryScope) throws SQLException {
		if ( factoryScope.getSessionFactory().getJdbcServices().getDialect().getTimeZoneSupport() == TimeZoneSupport.NATIVE ) {
			// Oracle and H2 require reading/writing through OffsetDateTime to avoid TZ related miscalculations
			return Timestamp.from( resultSet.getObject( position, OffsetDateTime.class ).toInstant() );
		}
		else {
			return resultSet.getTimestamp( position, Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ) );
		}
	}

	@Override
	protected void bindJdbcValue(
			PreparedStatement statement,
			int position,
			SessionFactoryScope factoryScope) throws SQLException {
		if ( factoryScope.getSessionFactory().getJdbcServices().getDialect().getTimeZoneSupport() == TimeZoneSupport.NATIVE ) {
			// Oracle and H2 require reading/writing through OffsetDateTime to avoid TZ related miscalculations
			statement.setObject(
					position,
					testParam.data().asInstant().atOffset( ZoneOffset.UTC )
			);
		}
		else {
			statement.setTimestamp(
					position,
					testParam.data().asTimestamp(),
					Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) )
			);
		}
	}

	public record InstantData(int year, int month, int day,
							int hour, int minute, int second, int nanosecond) implements Data<Instant> {

		public Instant asInstant() {
			return OffsetDateTime.of( year, month, day, hour, minute, second, nanosecond, ZoneOffset.UTC ).toInstant();
		}

		public Timestamp asTimestamp() {
			return Timestamp.from( asInstant() );
		}

		@Override
		public Instant makeValue() {
			return asInstant();
		}
	}

	private static class ParametersBuilder extends AbstractParametersBuilder<Instant, InstantData, ParametersBuilder> {
		public ParametersBuilder(Dialect dialect) {
			super( dialect );
		}

		public ParametersBuilder add(int year, int month, int day,
									int hour, int minute, int second, int nanosecond,
									ZoneId defaultTimeZone) {
			if ( !isNanosecondPrecisionSupported() ) {
				nanosecond = 0;
			}
			return add( defaultTimeZone, new InstantData( year, month, day, hour, minute, second, nanosecond ) );
		}
	}

	protected Instant createInstant(int year, int month, int day, int hour, int minute, int second, int nanosecond) {
		return OffsetDateTime.of( year, month, day, hour, minute, second, nanosecond, ZoneOffset.UTC ).toInstant();
	}

	protected Timestamp createTimestamp(int year, int month, int day, int hour, int minute, int second, int nanosecond) {
		return Timestamp.from( createInstant( year, month, day, hour, minute, second, nanosecond ) );
	}

	protected void setJdbcValueForNonHibernateWrite(
			PreparedStatement statement,
			int parameterIndex,
			Instant instant,
			SessionFactoryScope factoryScope) throws SQLException {
		if ( factoryScope.getSessionFactory().getJdbcServices().getDialect().getTimeZoneSupport() == TimeZoneSupport.NATIVE ) {
			// Oracle and H2 require reading/writing through OffsetDateTime to avoid TZ related miscalculations
			statement.setObject( parameterIndex, instant.atOffset( ZoneOffset.UTC ) );
		}
		else {
			statement.setTimestamp(
					parameterIndex,
					Timestamp.from( instant ),
					Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) )
			);
		}
	}

	protected Object getActualJdbcValue(ResultSet resultSet, int columnIndex, SessionFactoryScope factoryScope) throws SQLException {
		if ( factoryScope.getSessionFactory().getJdbcServices().getDialect().getTimeZoneSupport() == TimeZoneSupport.NATIVE ) {
			// Oracle and H2 require reading/writing through OffsetDateTime to avoid TZ related miscalculations
			return Timestamp.from( resultSet.getObject( columnIndex, OffsetDateTime.class ).toInstant() );
		}
		else {
			return resultSet.getTimestamp( columnIndex, Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ) );
		}
	}

	@Entity
	@Table(name = ENTITY_TBL_NAME)
	public static class EntityWithInstant {
		@Id
		@Column(name = ID_COLUMN_NAME)
		private Integer id;

		@Basic
		@Column(name = VALUE_COLUMN_NAME)
		private Instant value;

		protected EntityWithInstant() {
		}

		private EntityWithInstant(int id, Instant value) {
			this.id = id;
			this.value = value;
		}

		public Integer getId() {
			return id;
		}

		public Instant getValue() {
			return value;
		}
	}
}
