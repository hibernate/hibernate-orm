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
import org.hamcrest.MatcherAssert;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.StandardBasicTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.hamcrest.core.Is.is;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;
import static org.hibernate.cfg.MappingSettings.TIMEZONE_DEFAULT_STORAGE;
import static org.hibernate.orm.test.type.temporal.Timezones.ZONE_AMSTERDAM;
import static org.hibernate.orm.test.type.temporal.Timezones.ZONE_AUCKLAND;
import static org.hibernate.orm.test.type.temporal.Timezones.ZONE_GMT;
import static org.hibernate.orm.test.type.temporal.Timezones.ZONE_OSLO;
import static org.hibernate.orm.test.type.temporal.Timezones.ZONE_PARIS;
import static org.hibernate.orm.test.type.temporal.Timezones.ZONE_UTC_MINUS_8;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-10372")
@TestInstance( TestInstance.Lifecycle.PER_METHOD )
@ParameterizedClass
@MethodSource("testData")
@DomainModel(annotatedClasses = OffsetDateTimeTest.EntityWithOffsetDateTime.class)
@SessionFactory
public class OffsetDateTimeTest extends AbstractJavaTimeTypeTests<OffsetDateTime, OffsetDateTimeTest.EntityWithOffsetDateTime> {

	public static List<Parameter<OffsetDateTime,DataImpl>> testData() {
		return new ParametersBuilder( DialectContext.getDialect()  )
				// Not affected by any known bug
				.add( 2017, 11, 6, 19, 19, 1, 0, "+10:00", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+07:00", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+01:30", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+01:00", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+00:30", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "-02:00", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "-06:00", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "-08:00", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+10:00", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+07:00", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+01:30", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 500, "+01:00", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+01:00", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+00:30", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "-02:00", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "-06:00", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "-08:00", ZONE_PARIS )
				.skippedForDialects(
						// MySQL/Mariadb cannot store values equal to epoch exactly, or less, in a timestamp.
						Arrays.asList( MySQLDialect.class, MariaDBDialect.class ),
						b -> b
								// Not affected by any known bug
								.add( 1970, 1, 1, 0, 0, 0, 0, "+01:00", ZONE_GMT )
								.add( 1970, 1, 1, 0, 0, 0, 0, "+00:00", ZONE_GMT )
								.add( 1970, 1, 1, 0, 0, 0, 0, "-01:00", ZONE_GMT )
								.add( 1900, 1, 1, 0, 0, 0, 0, "+01:00", ZONE_GMT )
								.add( 1900, 1, 1, 0, 0, 0, 0, "+00:00", ZONE_GMT )
								.add( 1900, 1, 1, 0, 0, 0, 0, "-01:00", ZONE_GMT )
								.add( 1900, 1, 1, 0, 0, 0, 0, "+00:00", ZONE_OSLO )
								.add( 1900, 1, 1, 0, 9, 21, 0, "+00:09:21", ZONE_PARIS )
								.add( 1900, 1, 1, 0, 19, 32, 0, "+00:19:32", ZONE_PARIS )
								.add( 1900, 1, 1, 0, 19, 32, 0, "+00:19:32", ZONE_AMSTERDAM )
								.add( 1900, 1, 1, 0, 9, 20, 0, "+00:09:21", ZONE_PARIS )
								.add( 1900, 1, 1, 0, 19, 31, 0, "+00:19:32", ZONE_PARIS )
								.add( 1900, 1, 1, 0, 19, 31, 0, "+00:19:32", ZONE_AMSTERDAM )
				)
				.skippedForDialects(
						// MySQL/Mariadb cannot store values equal to epoch exactly, or less, in a timestamp.
						dialect -> dialect instanceof MySQLDialect || dialect instanceof MariaDBDialect
								|| dialect instanceof H2Dialect && ( (H2Dialect) dialect ).hasOddDstBehavior(),
						b -> b
								// Affected by HHH-13266 (JDK-8061577)
								.add( 1892, 1, 1, 0, 0, 0, 0, "+00:00", ZONE_OSLO )
				)
				.skippedForDialects(
						// MySQL/Mariadb/Sybase cannot store dates in 1600 in a timestamp.
						dialect -> dialect instanceof MySQLDialect || dialect instanceof MariaDBDialect || dialect instanceof SybaseDialect
							|| dialect instanceof H2Dialect && ( (H2Dialect) dialect ).hasOddDstBehavior(),
						b -> b
								.add( 1600, 1, 1, 0, 0, 0, 0, "+00:19:32", ZONE_AMSTERDAM )
				)
				// HHH-13379: DST end (where Timestamp becomes ambiguous, see JDK-4312621)
				// => This used to work correctly in 5.4.1.Final and earlier
				.skippedForDialects(
						dialect -> dialect instanceof H2Dialect && ( (H2Dialect) dialect ).hasOddDstBehavior(),
						b -> b.add( 2018, 10, 28, 2, 0, 0, 0, "+01:00", ZONE_PARIS )
								.add( 2018, 4, 1, 2, 0, 0, 0, "+12:00", ZONE_AUCKLAND )
				)
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

	private final Parameter<OffsetDateTime,DataImpl> testParam;

	public OffsetDateTimeTest(Parameter<OffsetDateTime,DataImpl> testParam) {
		super( testParam.env() );
		this.testParam = testParam;
	}

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		builder.applySetting( TIMEZONE_DEFAULT_STORAGE, NORMALIZE );
		return super.produceServiceRegistry( builder );
	}

	@Override
	protected Class<EntityWithOffsetDateTime> getEntityType() {
		return EntityWithOffsetDateTime.class;
	}

	@Override
	protected EntityWithOffsetDateTime createEntityForHibernateWrite(int id) {
		return new EntityWithOffsetDateTime( id, getOriginalOffsetDateTime() );
	}

	private OffsetDateTime getOriginalOffsetDateTime() {
		return testParam.data().makeValue();
	}

	@Override
	protected OffsetDateTime getExpectedPropertyValueAfterHibernateRead() {
		return getOriginalOffsetDateTime().atZoneSameInstant( ZoneId.systemDefault() ).toOffsetDateTime();
	}

	@Override
	protected OffsetDateTime getActualPropertyValue(EntityWithOffsetDateTime entity) {
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
		LocalDateTime dateTimeInDefaultTimeZone = getOriginalOffsetDateTime().atZoneSameInstant( ZoneId.systemDefault() )
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
	protected Object extractJdbcValue(
			ResultSet resultSet,
			int columnIndex,
			SessionFactoryScope factoryScope) throws SQLException {
		return resultSet.getTimestamp( columnIndex );
	}

	@Test
	public void testRetrievingEntityByOffsetDateTime(SessionFactoryScope factoryScope) {
		Timezones.withDefaultTimeZone( testParam.env(), () -> {
			factoryScope.inTransaction( session -> {
				session.persist( new EntityWithOffsetDateTime( 1, getOriginalOffsetDateTime() ) );
			} );
			Consumer<OffsetDateTime> checkOneMatch = expected -> factoryScope.inSession( s -> {
				var list = s.createQuery( "from EntityWithOffsetDateTime o where o.value = :date" )
						.setParameter( "date", expected, StandardBasicTypes.OFFSET_DATE_TIME )
						.list();
				MatcherAssert.assertThat( list.size(), is( 1 ) );
			} );
			checkOneMatch.accept( getOriginalOffsetDateTime() );
			checkOneMatch.accept( getExpectedPropertyValueAfterHibernateRead() );
			checkOneMatch.accept( getExpectedPropertyValueAfterHibernateRead().withOffsetSameInstant( ZoneOffset.UTC ) );
		} );
	}

	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	@Entity(name = "EntityWithOffsetDateTime")
	@Table(name = ENTITY_TBL_NAME)
	public static class EntityWithOffsetDateTime {
		@Id
		@Column(name = ID_COLUMN_NAME)
		private Integer id;

		@Basic
		@Column(name = VALUE_COLUMN_NAME)
		private OffsetDateTime value;

		protected EntityWithOffsetDateTime() {
		}

		private EntityWithOffsetDateTime(int id, OffsetDateTime value) {
			this.id = id;
			this.value = value;
		}
	}

	private static class ParametersBuilder extends AbstractParametersBuilder<OffsetDateTime, DataImpl, ParametersBuilder> {
		protected ParametersBuilder(Dialect dialect) {
			super( dialect );
		}

		public ParametersBuilder add(int year, int month, int day,
									int hour, int minute, int second, int nanosecond, String offset, ZoneId defaultTimeZone) {
			if ( !isNanosecondPrecisionSupported() ) {
				nanosecond = 0;
			}
			return add( defaultTimeZone, new DataImpl( year, month, day, hour, minute, second, nanosecond, offset ) );
		}
	}

	public record DataImpl(
			int year, int month, int day,
			int hour, int minute, int second, int nanosecond,
			String offset) implements Data<OffsetDateTime> {
		@Override
		public OffsetDateTime makeValue() {
			return OffsetDateTime.of( year, month, day, hour, minute, second, nanosecond, ZoneOffset.of( offset ) );
		}
	}
}
