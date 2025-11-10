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
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.SybaseDialect;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import static org.hibernate.orm.test.type.temporal.Timezones.ZONE_AMSTERDAM;
import static org.hibernate.orm.test.type.temporal.Timezones.ZONE_AUCKLAND;
import static org.hibernate.orm.test.type.temporal.Timezones.ZONE_GMT;
import static org.hibernate.orm.test.type.temporal.Timezones.ZONE_OSLO;
import static org.hibernate.orm.test.type.temporal.Timezones.ZONE_PARIS;
import static org.hibernate.orm.test.type.temporal.Timezones.ZONE_UTC_MINUS_8;

/**
 * Tests for storage of LocalDateTime properties.
 */
@TestInstance( TestInstance.Lifecycle.PER_METHOD )
@ParameterizedClass
@MethodSource("testData")
@DomainModel(annotatedClasses = LocalDateTimeTest.EntityWithLocalDateTime.class)
@SessionFactory
public class LocalDateTimeTest
		extends AbstractJavaTimeTypeTests<LocalDateTime, LocalDateTimeTest.EntityWithLocalDateTime> {

	public static List<Parameter<LocalDateTime, LocalDateTimeTest.DataImpl>> testData() {
		return new ParametersBuilder( DialectContext.getDialect() )
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
								.add( 1900, 1, 2, 0, 9, 21, 0, ZONE_PARIS )
								.add( 1900, 1, 2, 0, 19, 32, 0, ZONE_AMSTERDAM )
								.add( 1900, 1, 1, 0, 9, 20, 0, ZONE_PARIS )
								.add( 1900, 1, 1, 0, 19, 31, 0, ZONE_AMSTERDAM )
				)
				.skippedForDialects(
						// MySQL/Mariadb cannot store values equal to epoch exactly, or less, in a timestamp.
						dialect -> dialect instanceof MySQLDialect || dialect instanceof MariaDBDialect
								|| dialect instanceof H2Dialect && ( (H2Dialect) dialect ).hasOddDstBehavior(),
						b -> b
								// Affected by HHH-13266 (JDK-8061577)
								.add( 1892, 1, 1, 0, 0, 0, 0, ZONE_OSLO )
				)
				.skippedForDialects(
						// MySQL/Mariadb/Sybase cannot store dates in 1600 in a timestamp.
						dialect -> dialect instanceof MySQLDialect || dialect instanceof MariaDBDialect || dialect instanceof SybaseDialect
								|| dialect instanceof H2Dialect && ( (H2Dialect) dialect ).hasOddDstBehavior(),
						b -> b
								.add( 1600, 1, 1, 0, 0, 0, 0, ZONE_AMSTERDAM )
				)
				// HHH-13379: DST end (where Timestamp becomes ambiguous, see JDK-4312621)
				// It doesn't seem that any LocalDateTime can be affected by HHH-13379, but we add some tests just in case
				.add( 2018, 10, 28, 1, 0, 0, 0, ZONE_PARIS )
				.skippedForDialects(
						dialect -> dialect instanceof H2Dialect && ( (H2Dialect) dialect ).hasOddDstBehavior(),
						b -> b
								.add( 2018, 10, 28, 2, 0, 0, 0, ZONE_PARIS )
				)
				.add( 2018, 10, 28, 3, 0, 0, 0, ZONE_PARIS )
				.add( 2018, 10, 28, 4, 0, 0, 0, ZONE_PARIS )
				.add( 2018, 4, 1, 1, 0, 0, 0, ZONE_AUCKLAND )
				.skippedForDialects(
						dialect -> dialect instanceof H2Dialect && ( (H2Dialect) dialect ).hasOddDstBehavior(),
						b -> b
								.add( 2018, 4, 1, 2, 0, 0, 0, ZONE_AUCKLAND )
				)
				.add( 2018, 4, 1, 3, 0, 0, 0, ZONE_AUCKLAND )
				.add( 2018, 4, 1, 4, 0, 0, 0, ZONE_AUCKLAND )
				// => Also test DST start
				// This does not work, but it's unrelated to HHH-13379; see HHH-13515
				//.add( 2018, 3, 25, 2, 0, 0, 0, ZONE_PARIS )
				.add( 2018, 3, 25, 3, 0, 0, 0, ZONE_PARIS )
				// This does not work, but it's unrelated to HHH-13379; see HHH-13515
				//.add( 2018, 9, 30, 2, 0, 0, 0, ZONE_AUCKLAND )
				.add( 2018, 9, 30, 3, 0, 0, 0, ZONE_AUCKLAND )
				.build();
	}

	private final Parameter<LocalDateTime, LocalDateTimeTest.DataImpl> testParam;

	public LocalDateTimeTest(Parameter<LocalDateTime, LocalDateTimeTest.DataImpl> testParam) {
		super( testParam.env() );
		this.testParam = testParam;
	}

	@Override
	protected Class<EntityWithLocalDateTime> getEntityType() {
		return EntityWithLocalDateTime.class;
	}

	@Override
	protected EntityWithLocalDateTime createEntityForHibernateWrite(int id) {
		return new EntityWithLocalDateTime( id, getExpectedPropertyValueAfterHibernateRead() );
	}

	@Override
	protected LocalDateTime getExpectedPropertyValueAfterHibernateRead() {
		return testParam.data().makeValue();
	}

	@Override
	protected LocalDateTime getActualPropertyValue(EntityWithLocalDateTime entity) {
		return entity.value;
	}

	@Override
	protected void bindJdbcValue(
			PreparedStatement statement,
			int parameterIndex,
			SessionFactoryScope factoryScope) throws SQLException {
		statement.setTimestamp( parameterIndex, getExpectedJdbcValueAfterHibernateWrite() );
	}

	@Override
	protected Timestamp getExpectedJdbcValueAfterHibernateWrite() {
		return testParam.data().asTimestamp();
	}

	@Override
	protected Object extractJdbcValue(
			ResultSet resultSet,
			int columnIndex,
			SessionFactoryScope factoryScope) throws SQLException {
		return resultSet.getTimestamp( columnIndex );
	}

	@Entity
	@Table(name = ENTITY_TBL_NAME)
	public static final class EntityWithLocalDateTime {
		@Id
		@Column(name = ID_COLUMN_NAME)
		private Integer id;

		@Basic
		@Column(name = VALUE_COLUMN_NAME)
		private LocalDateTime value;

		protected EntityWithLocalDateTime() {
		}

		private EntityWithLocalDateTime(int id, LocalDateTime value) {
			this.id = id;
			this.value = value;
		}
	}

	private static class ParametersBuilder
			extends AbstractParametersBuilder<LocalDateTime, DataImpl, ParametersBuilder> {
		protected ParametersBuilder(Dialect dialect) {
			super( dialect );
		}

		public ParametersBuilder add(int year, int month, int day,
									int hour, int minute, int second, int nanosecond, ZoneId defaultTimeZone) {
			if ( !isNanosecondPrecisionSupported() ) {
				nanosecond = 0;
			}
			return add( defaultTimeZone, new DataImpl( year, month, day, hour, minute, second, nanosecond ) );
		}
	}

	public record DataImpl(
			int year,
			int month,
			int day,
			int hour,
			int minute,
			int second,
			int nanosecond) implements Data<LocalDateTime> {
		@Override
		public LocalDateTime makeValue() {
			return LocalDateTime.of( year, month, day, hour, minute, second, nanosecond );
		}

		public Timestamp asTimestamp() {
			return new Timestamp( year - 1900, month - 1, day, hour, minute, second, nanosecond );
		}
	}
}
